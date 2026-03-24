package dev.pompilius.auth.infrastructure.controllers

import dev.pompilius.auth.infrastructure.writers.{MailTokenWriter, SessionWriter}
import dev.pompilius.shared.domain._
import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
import dev.pompilius.users.domain._
import dev.pompilius.auth.domain.{MailToken, SessionId, SessionRepository}
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.i18n.MessagesImpl
import play.api.mvc.{Action, AnyContent}
import dev.pompilius.Strings
import dev.pompilius.auth.application.{LoginValidator, SessionCreator}
import dev.pompilius.auth.infrastructure.parsers.{LoginAsRequestParser, LoginRequestParser, MailTokenParser, PasswordResetRequestParser, SendPasswordResetMailRequestParser}
import dev.pompilius.mail.domain.{Mail, MailAddress, MailContent, MailSubject}
import dev.pompilius.mail.infrastructure.repositories.MailSmtpRepository
import dev.pompilius.shared.domain.exceptions.{BadRequestException, UnauthorizedException}
import org.apache.pekko.Done

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthController @Inject() (
    loginValidator: LoginValidator,
    sessionCreator: SessionCreator,
    sessionWriter: SessionWriter,
    userRepository: UserRepository,
    sessionRepository: SessionRepository,
    clock: Clock,
    configuration: Configuration,
    requestLogger: RequestLogger,
    mailTokenWriter: MailTokenWriter,
    mailSmtpRepository: MailSmtpRepository,
    implicit val cacheApi: AsyncCacheApi
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

  // Devuelve todos los datos de la sesión actual: User, Session, Roles
  def me: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (session, user, roles) =>
          for {
            sessionJs <- sessionWriter.toJson(session, user, roles)
          } yield {
            Ok(sessionJs)
          }
      }
    }

  // Cierra la sesión y la marca como borrada
  def logout: Action[AnyContent] =
    Action.async { implicit request =>
      for {
        currentSession <- findCurrentSession
        _ <- currentSession match {
          case Some(session) =>
            sessionRepository.save(session.copy(deleted = true))
          case _ =>
            Future.unit
        }
      } yield {
        Ok.removingFromSession(Strings.sessionId)
      }
    }

  // Permite a un usuario iniciar sesión con un nombre de usuario y contraseña
  def login: Action[AnyContent] =
    Action.async { implicit request =>
      withLimit(
        maxRequest = configuration.auth.maxRequest,
        timeWindow = configuration.auth.timeWindow,
        key = None
      ) { () =>
        val loginRequest = LoginRequestParser.parse(request)

        for {
          session <-
            loginValidator
              .validateLoginAndPassword(
                loginPassword = loginRequest,
                requestFingerprint = getFingerprint
              )

          user <- userRepository.getById(session.userId)

          // EL usuario puede existir, pero no estar habilitado, en ese caso no se le permite iniciar sesión
          _ = if (!user.enabled) throw new UnauthorizedException("User is not enabled")

          // Obtenemos los roles del usuario
          roles <- findRolesByUser(
            user = user
          )
          _ = if (roles.isEmpty && !configuration.auth.allowLoginWithoutRoles) {
            throw new UnauthorizedException("User does not have any roles assigned")
          }
          userSessionJson <- sessionWriter.toJson(session, user, roles)
        } yield {
          Ok(userSessionJson)
            .addingToSession(
              //Recordar que play cifra las cookies, por lo que no es un gran problema guardar el userId en la sesión, y nos ahorramos tener que hacer una consulta a la base de datos para obtenerlo cada vez que el usuario hace una petición
              Strings.userId -> session.userId.toString,
              Strings.sessionId -> session.id.toString
            )
        }
      }
    }

  // Con la intención de dar soporte, y solucionar incidencias, a los administradores se les da el permiso de
  // iniciar session como otro usuario, solo necesita conocer su username.
  def loginAs: Action[AnyContent] =
    Action.async { implicit request =>
      findCurrentUser.flatMap {
        case Some(admin) =>
          findRolesByUser(admin).flatMap { roles =>
            if (roles.contains(Role.ADMIN)) {
              val loginAsRequest = LoginAsRequestParser.parse(request)
              for {
                session <- sessionCreator.loginAs(
                  loginAsRequest = loginAsRequest,
                  requestFingerprint = getFingerprint
                )
                user <- userRepository.getById(session.userId)
                userRoles <- findRolesByUser(user)
                userSessionJson <- sessionWriter.toJson(session, user, userRoles)
                _ <- requestLogger.log(
                  RequestLog(
                    id = RequestLogId.gen(configuration.nodeId),
                    userId = admin.id,
                    timestamp = clock.now,
                    address = request.remoteAddress,
                    method = request.method,
                    path = request.path,
                    body = request.body.asJson.map(_.toString),
                    metadata = None
                  )
                )
              } yield {
                Ok(userSessionJson)
                  .addingToSession(
                    Strings.userId -> session.userId.toString,
                    Strings.sessionId -> session.id.toString
                  )
              }
            } else {
              throw new UnauthorizedException("You are not allowed to perform this action")
            }
          }
        case None =>
          Future.successful(Unauthorized("No autenticado"))
      }
    }

  def resetPassword: Action[AnyContent] =
    Action.async { implicit request =>
      withLimit(
        maxRequest = configuration.auth.maxRequest,
        timeWindow = configuration.auth.timeWindow,
        key = None
      ) { () =>
        val passwordResetRequest = PasswordResetRequestParser.parse(request)
        val mailToken = MailTokenParser.parse(passwordResetRequest.token, configuration.mails.tokenSecretKey)

        if (mailToken.expires.isBefore(clock.now))
          throw new BadRequestException("Token expired")

        for {
          // Buscamos si hay un usuario con ese email
          currentUser <-
            userRepository
              .findByEmail(mailToken.mail)
              .map(_.getOrElse(throw new UnauthorizedException("User not found")))

          // Si lo hay, cambiamos su contraseña
          updatedUser = currentUser.copy(
            passwordHash = UserPassword(passwordResetRequest.newPassword).hash
          )

          _ <- userRepository.save(updatedUser)

          _ <-
            if (passwordResetRequest.closeAllSessions) {
              sessionRepository.closeAllSessions(updatedUser.id, None)
            } else {
              Future.unit
            }

          // Registramos la acción en el log de peticiones
          _ <- requestLogger.log(
            RequestLog(
              id = RequestLogId.gen(configuration.nodeId),
              userId = currentUser.id,
              timestamp = clock.now,
              address = request.remoteAddress,
              method = request.method,
              path = request.path,
              body = None,
              metadata = None
            )
          )

          _ = if (!updatedUser.enabled) throw new UnauthorizedException("User is not enabled")

          // Creamos una nueva sesión
          session <- sessionCreator.create(updatedUser, getFingerprint)

          roles <- findRolesByUser(user = updatedUser)

          userSessionJson <- sessionWriter.toJson(session, updatedUser, roles)
        } yield {
          Ok(userSessionJson)
            .addingToSession(
              Strings.userId -> session.userId.toString,
              Strings.sessionId -> session.id.toString
            )
        }
      }
    }

  def sendPasswordResetMail: Action[AnyContent] =
    Action.async { implicit request =>
      withLimit(
        maxRequest = configuration.auth.maxRequest,
        timeWindow = configuration.auth.timeWindow,
        key = None
      ) { () =>
        val sendPasswordResetMailRequest = SendPasswordResetMailRequestParser.parse(request)

        for {
          // Buscamos si hay un usuario con ese email
          currentUser <-
            userRepository
              .findByEmail(sendPasswordResetMailRequest.email)

          _ <- currentUser match {
            // Si el usuario existe y esta activo
            case Some(user) if user.enabled =>
              mailTokenWriter
                .toString(
                  MailToken(user.email, clock.now.plusMillis(configuration.auth.resetLinkDuration.toMillis.toInt)),
                  configuration.mails.tokenSecretKey
                ).flatMap { token =>
                  // Creamos un token y lo enviamos por email
                  val messages = MessagesImpl(getLanguage, messagesApi)
                  val mailAddress = MailAddress(address = user.email, name = None)
                  // Creamos el link de autenticación, que ira en el correo.
                  val link = UrlUtil.addQueryParameters(configuration.auth.resetPasswordUrl, Map(Strings.token -> token))
                  // Creamos el contenido del email usando una plantilla HTML
                  val mailContent =
                    dev.pompilius.auth.infrastructure.views.html.reset_password_email(link)(messages)

                  val mail = Mail(
                    to = mailAddress,
                    subject = Some(MailSubject(messages("mail.reset.subject"))),
                    content = MailContent(
                      text = None,
                      html = Some(mailContent.body)
                    )
                  )

                  mailSmtpRepository.sendMail(mail)
                }

            case _ =>
              // Si no existe o no esta activo, no hacemos nada
              Future.unit
          }

        } yield {
          Ok
        }
      }
    }
}
