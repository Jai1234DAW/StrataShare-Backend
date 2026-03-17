//package dev.pompilius.auth.infrastructure.controllers
//
//import dev.pompilius.Strings
//import dev.tnr.elback.auth.application.{LoginValidator, SessionCreator}
//import dev.pompilius.auth.domain._
//import dev.tnr.elback.auth.infrastructure.parsers._
//import dev.tnr.elback.auth.infrastructure.writers.{MailTokenWriter, UserSessionWriter}
//import dev.tnr.elback.mail.domain._
//import dev.pompilius.shared.domain._
//import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
//import dev.pompilius.user.domain._
//import play.api.Logger
//import play.api.cache.AsyncCacheApi
//import play.api.i18n.MessagesImpl
//import play.api.mvc.{Action, AnyContent}
//
//import javax.inject._
//import scala.concurrent.{ExecutionContext, Future}
//
//@Singleton
//class AuthController @Inject() (
//    loginValidator: LoginValidator,
//    sessionCreator: SessionCreator,
//    userSessionWriter: UserSessionWriter,
//    mailTokenWriter: MailTokenWriter,
//    userRepository: UserRepository,
//    sessionRepository: SessionRepository,
//    clock: Clock,
//    configuration: Configuration,
//    requestLogger: RequestLogger,
//    sendMailQueue: SendMailQueue,
//    implicit val cacheApi: AsyncCacheApi
//)(implicit ec: ExecutionContext)
//    extends BaseController {
//
//  private val logger = Logger(this.getClass)
//
//  // Devuelve todos los datos de la sesión actual: User, Session, Roles
//  def me: Action[AnyContent] =
//    Action.async { implicit request =>
//      withAuthenticatedUser {
//        case (session, user, roles) =>
//          for {
//            sessionJs <- userSessionWriter.toJson(user, session, roles)
//          } yield {
//            Ok(sessionJs)
//          }
//      }
//    }
//
//  // Cierra la sesión.
//  def logout: Action[AnyContent] =
//    Action.async { implicit request =>
//      Future.successful(
//        Ok.removingFromSession(Strings.userId, Strings.token)
//      )
//    }
//
//  // Muestra un formulario de login
//  def loginForm(redirectTo: Option[String]): Action[AnyContent] =
//    Action {
//      redirectTo match {
//        case Some(url) if UrlUtil.isValidRelativeUrl(url) =>
//          Ok(login_form(routes.AuthController.login.url, redirectTo))
//        case _ =>
//          Ok(login_form(routes.AuthController.login.url, None))
//      }
//    }
//
//  // Permite a un usuario iniciar sesión con un nombre de usuario y contraseña
//  def login: Action[AnyContent] =
//    Action.async { implicit request =>
//      withLimit(
//        maxRequest = configuration.auth.maxRequest,
//        timeWindow = configuration.auth.timeWindow,
//        key = None
//      ) { () =>
//        val loginRequest = LoginRequestParser.parse(request)
//
//        for {
//          session <-
//            loginValidator
//              .validateLoginAndPassword(
//                loginPassword = loginRequest,
//                requestFingerprint = getFingerprint
//              )
//
//          user <- userRepository.getById(session.userId)
//
//          _ = if (!user.enabled) throw new UnauthorizedException("User is not enabled")
//
//          permissions <- userPermissionRepository.findAllByUserId(user.id)
//
//          userSessionJson <- userSessionWriter.toJson(user, session, permissions)
//        } yield {
//          Ok(userSessionJson)
//            .addingToSession(
//              Strings.userId -> session.userId.toString,
//              Strings.token -> session.token.toString
//            )
//        }
//      }
//    }
//
//  // Con la intención de dar soporte, y solucionar incidencias, a algunos administradores se les da el permiso de
//  // iniciar session como otro usuario, solo necesita conocer su username.
//  def loginAs: Action[AnyContent] =
//    Action.async { implicit request =>
//      withThisPermissions(Set(Permission.LOGIN_AS)) {
//        case (_, manager, _) =>
//          val loginAsRequest = LoginAsRequestParser.parse(request)
//
//          for {
//
//            session <- sessionCreator.loginAs(
//              loginAsRequest = loginAsRequest,
//              requestFingerprint = getFingerprint
//            )
//
//            user <- userRepository.getById(session.userId)
//
//            _ = checkLevels(manager, user)
//
//            permissions <- userPermissionRepository.findAllByUserId(user.id)
//
//            userSessionJson <- userSessionWriter.toJson(user, session, permissions)
//
//            // Que un administrador inicie sesión suplantando a otro usuario es un hecho excepcional
//            // que, por seguridad, debe ser registrado
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = manager.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//          } yield {
//            Ok(userSessionJson)
//              .addingToSession(
//                Strings.userId -> session.userId.toString,
//                Strings.token -> session.token.toString
//              )
//          }
//      }
//    }
//
//  def resetPassword: Action[AnyContent] =
//    Action.async { implicit request =>
//      withLimit(
//        maxRequest = configuration.auth.maxRequest,
//        timeWindow = configuration.auth.timeWindow,
//        key = None
//      ) { () =>
//        val passwordResetRequest = PasswordResetRequestParser.parse(request)
//        val mailToken = MailTokenParser.parse(passwordResetRequest.token, configuration.mails.tokenSecretKey)
//
//        if (mailToken.expires.isBefore(clock.now))
//          throw new BadRequestException("Token expired")
//
//        for {
//          // Buscamos si hay un usuario con ese email
//          currentUser <-
//            userRepository
//              .findByEmail(mailToken.mail)
//              .map(_.getOrElse(throw new UnauthorizedException("User not found")))
//
//          // Si lo hay, cambiamos su contraseña
//          updatedUser = currentUser.copy(
//            passwordHash = UserPassword(passwordResetRequest.newPassword).hash
//          )
//
//          _ <- userRepository.save(updatedUser)
//
//          _ <-
//            if (passwordResetRequest.closeAllSessions) {
//              sessionRepository.closeAllSessions(updatedUser.id, None)
//            } else {
//              Future.unit
//            }
//
//          _ <- requestLogger.log(
//            RequestLog(
//              id = RequestLogId.gen(config.nodeId),
//              userId = currentUser.id,
//              timestamp = clock.now,
//              address = request.remoteAddress,
//              method = request.method,
//              path = request.path,
//              body = None,
//              metadata = None
//            )
//          )
//
//          _ = if (!updatedUser.enabled) throw new UnauthorizedException("User is not enabled")
//
//          // Creamos una nueva sesión
//          session <- sessionCreator.create(updatedUser, getFingerprint)
//
//          permissions <- userPermissionRepository.findAllByUserId(updatedUser.id)
//
//          userSessionJson <- userSessionWriter.toJson(updatedUser, session, permissions)
//        } yield {
//          Ok(userSessionJson)
//            .addingToSession(
//              Strings.userId -> session.userId.toString,
//              Strings.token -> session.token.toString
//            )
//        }
//      }
//    }
//
//  def sendPasswordResetMail: Action[AnyContent] =
//    Action.async { implicit request =>
//      withLimit(
//        maxRequest = configuration.auth.maxRequest,
//        timeWindow = configuration.auth.timeWindow,
//        key = None
//      ) { () =>
//        val sendPasswordResetMailRequest = SendPasswordResetMailRequestParser.parse(request)
//
//        for {
//          // Buscamos si hay un usuario con ese email
//          currentUser <-
//            userRepository
//              .findByEmail(sendPasswordResetMailRequest.email)
//
//          _ <- currentUser match {
//            // Si el usuario existe y esta activo
//            case Some(user) if user.enabled =>
//              mailTokenWriter
//                .toString(
//                  MailToken(user.email, clock.now.plusMillis(configuration.auth.resetLinkDuration.toMillis.toInt)),
//                  configuration.mails.tokenSecretKey
//                ) map { token =>
//                // Creamos un token y lo enviamos por email
//                val messages = MessagesImpl(getLanguage, messagesApi)
//                val mailAddress = MailAddress(address = user.email, name = None)
//                // Creamos el link de autenticación, que ira en el correo.
//                val link = UrlUtil.addQueryParameters(configuration.auth.resetPasswordUrl, Map(Strings.token -> token))
//                // Creamos el contenido del email de pago usando una plantilla
//                val mailContent =
//                  dev.tnr.elback.auth.infrastructure.views.html.reset_password_email(link)(messages)
//
//                val mail = Mail(
//                  to = mailAddress,
//                  subject = Some(MailSubject(messages("mail.reset.subject"))),
//                  content = MailContent(
//                    contentType = mailContent.contentType,
//                    content = mailContent.body
//                  )
//                )
//
//                // Enviamos el email
//                sendMailQueue.add(mail = mail, accountId = None, key = None)
//              }
//
//            case _ =>
//              // Si no existe o no esta activo, no hacemos nada
//              Future.unit
//          }
//
//        } yield {
//          Ok
//        }
//      }
//    }
//
//  // Comprueba que el usuario que hace la petición tiene un nivel igual o superior al usuario que quiere modificar
//  private def checkLevels(admin: User, user: User): Unit = {
//    if (user.level > admin.level) {
//      throw new ForbiddenException("The user is on a higher level")
//    }
//  }
//
//}
