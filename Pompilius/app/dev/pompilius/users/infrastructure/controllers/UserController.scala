//package dev.pompilius.user.infrastructure.controllers
//
//import play.api.i18n.MessagesImpl
//import play.api.libs.json.{JsValue, Json}
//import play.api.mvc.{Action, AnyContent, InjectedController}
//
//import javax.inject._
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.{Success, Try}
//
//@Singleton
//class UserController @Inject() (
//    imageCheck: ImageCheck,
//    userWriter: UserWriter,
//    mailTokenWriter: MailTokenWriter,
//    requestLogger: RequestLogger,
//    userRepository: UserRepository,
//    sessionRepository: SessionRepository,
//    clock: Clock,
//    configuration: Configuration,
//    sendMailQueue: SendMailQueue
//)(implicit ec: ExecutionContext)
//    extends InjectedController
//    with BaseController {
//
//  def updateUser(): Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val updateUserRequest = UpdateUserRequestParser.parse(request)
//
//          for {
//
//            _ <- userRepository.findByUsername(updateUserRequest.username).map {
//              // Si otro usuario esta usando este username, lanzamos una excepción
//              case Some(u) if u.id.id != currentUser.id.id =>
//                throw new UsernameAlreadyInUseException()
//              case _ =>
//            }
//
//            updatedUser = currentUser.copy(
//              username = updateUserRequest.username,
//              phone = updateUserRequest.phone,
//              firstName = updateUserRequest.firstName,
//              lastName = updateUserRequest.lastName,
//              country = updateUserRequest.country,
//              language = updateUserRequest.language,
//              bio = updateUserRequest.bio,
//              updated = clock.now
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = currentUser.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def updateSettings(): Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val updateSettingsRequest = UpdateSettingsRequestParser.parse(request)
//
//          val updatedUser = currentUser.copy(
//            notifyOnSecretReceived = updateSettingsRequest.notifyOnSecretReceived,
//            notifyOnFileReceived = updateSettingsRequest.notifyOnFileReceived,
//            notifyOnSecretOpened = updateSettingsRequest.notifyOnSecretOpened,
//            notifyOnFileDownload = updateSettingsRequest.notifyOnFileDownload,
//            updated = clock.now
//          )
//
//          for {
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = currentUser.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def changePassword: Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (currentSession, currentUser, _) =>
//          val changeUserPasswordRequest = ChangeUserPasswordRequestParser.parse(request)
//
//          if (!UserPassword(changeUserPasswordRequest.oldPassword).check(currentUser.passwordHash)) {
//            throw new InvalidPasswordOrUsernameException("The old password does not match")
//          }
//
//          val updatedUser = currentUser.copy(
//            passwordHash = UserPassword(changeUserPasswordRequest.newPassword).hash
//          )
//
//          for {
//            _ <- userRepository.save(updatedUser)
//
//            _ <-
//              if (changeUserPasswordRequest.closeAllSessions) {
//                sessionRepository.closeAllSessions(updatedUser.id, Some(currentSession.token))
//              } else {
//                Future.unit
//              }
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = currentUser.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def changeAvatar: Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val uploadedImageRequest = UploadedImageRequestParser.parse(request)
//
//          for {
//            _ <- imageCheck.check(uploadedImageRequest.id)
//
//            updatedUser = currentUser.copy(
//              avatar = Some(uploadedImageRequest.id)
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            updatedUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
//  def deleteAvatar(): Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val updatedUser = currentUser.copy(
//            avatar = None
//          )
//
//          for {
//            _ <- userRepository.save(updatedUser)
//
//            updatedUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
//  def changeHeader: Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val uploadedImageRequest = UploadedImageRequestParser.parse(request)
//
//          for {
//            _ <- imageCheck.check(uploadedImageRequest.id)
//
//            updatedUser = currentUser.copy(
//              header = Some(uploadedImageRequest.id)
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            updatedUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
//  def deleteHeader(): Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val updatedUser = currentUser.copy(
//            header = None
//          )
//
//          for {
//            _ <- userRepository.save(updatedUser)
//
//            updatedUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
//  def changeEmail: Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, currentUser, _) =>
//          val changeMailRequest = ChangeMailRequestParser.parse(request)
//          val mailToken = MailTokenParser.parse(changeMailRequest.token, configuration.mails.tokenSecretKey)
//
//          if (!mailToken.mail.equalsIgnoreCase(changeMailRequest.email)) {
//            throw new BadRequestException("The email does not match with the token")
//          }
//
//          for {
//            _ <- userRepository.findByEmail(mailToken.mail).map {
//              // Si otro usuario esta usando este email, lanzamos una excepción
//              case Some(u) if u.id.id != currentUser.id.id =>
//                throw new EmailAlreadyInUseException()
//              case _ =>
//            }
//
//            updatedUser = currentUser.copy(
//              email = mailToken.mail
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = currentUser.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = None,
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def sendChangeMail: Action[AnyContent] =
//    Action.async { implicit request =>
//      withSession {
//        case (_, _, _) =>
//          val sendMailChangeRequest = SendMailChangeRequestParser.parse(request)
//
//          for {
//            _ <-
//              mailTokenWriter
//                .toString(
//                  MailToken(
//                    sendMailChangeRequest.email,
//                    clock.now.plusMillis(configuration.users.changeMailLinkDuration.toMillis.toInt)
//                  ),
//                  configuration.mails.tokenSecretKey
//                ) map { token =>
//                // Creamos un token y lo enviamos por email
//                val messages = MessagesImpl(getLanguage, messagesApi)
//                val mailAddress = MailAddress(address = sendMailChangeRequest.email, name = None)
//                // Creamos el link de autenticación, que ira en el correo.
//                val link = UrlUtil.addQueryParameters(
//                  configuration.users.changeMailUrl,
//                  Map(Strings.email -> sendMailChangeRequest.email, Strings.token -> token)
//                )
//                // Creamos el contenido del email de pago usando una plantilla
//                val mailContent =
//                  dev.tnr.elback.user.infrastructure.views.html.change_email(link)(messages)
//
//                val mail = Mail(
//                  to = mailAddress,
//                  subject = Some(MailSubject(messages("mail.change.subject"))),
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
//          } yield {
//            Ok
//          }
//      }
//    }
//
//  def validateUsername: Action[JsValue] =
//    Action.async(parse.json) { implicit request =>
//      (request.body \ Strings.username).as[String] match {
//        case Strings.usernameRegex(username) =>
//          userRepository.findByUsername(username).map(_.isEmpty).map { available =>
//            // Si el nombre de usuario es válido, devolvemos su disponibilidad
//            Ok(Json.obj(Strings.available -> available, Strings.valid -> true))
//          }
//        case _ =>
//          // Si el nombre de usuario no es válido, devolvemos un error
//          Future.successful(Ok(Json.obj(Strings.available -> false, Strings.valid -> false)))
//      }
//    }
//
//  def validateEmail: Action[JsValue] =
//    Action.async(parse.json) { implicit request =>
//      (request.body \ Strings.email).as[String] match {
//        case Strings.emailRegex(email) =>
//          userRepository.findByEmail(email).map(_.isEmpty).map { available =>
//            // Si el email es válido, devolvemos su disponibilidad
//            Ok(Json.obj(Strings.available -> available, Strings.valid -> true))
//          }
//        case _ =>
//          // Si el email no es válido, devolvemos un error
//          Future.successful(Ok(Json.obj(Strings.available -> false, Strings.valid -> false)))
//      }
//    }
//
//  private def getUserByName(idOrUsername: String): Future[User] = {
//    userRepository.findByUsername(idOrUsername).flatMap {
//      case Some(u) =>
//        Future.successful(u)
//      case _ =>
//        Try(UserId(idOrUsername)) match {
//          case Success(userId) =>
//            userRepository.getById(userId)
//          case _ =>
//            throw new UserNotFoundException(s"User not found: ${idOrUsername.take(255)}")
//        }
//    }
//  }
//
//}
