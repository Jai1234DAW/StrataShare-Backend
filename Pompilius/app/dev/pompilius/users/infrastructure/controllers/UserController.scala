package dev.pompilius.users.infrastructure.controllers

import dev.pompilius.Strings
import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.attachment.infrastructure.parsers.UploadedAttachmentRequestParser
import dev.pompilius.auth.domain.exceptions.InvalidPasswordOrUsernameException
import dev.pompilius.shared.domain.exceptions.{BadRequestException, NotFoundException}
import dev.pompilius.users.infrastructure.writers.UserWriter
import dev.pompilius.users.domain._
import dev.pompilius.attachment.domain.AttachmentCheck
import play.api.libs.Files
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.Json
import dev.pompilius.users.domain.exceptions.{EmailAlreadyInUseException, UserNotFoundException, UsernameAlreadyInUseException}
import dev.pompilius.users.infrastructure.parsers.{ChangeMailRequestParser, ChangeUserPasswordRequestParser, RegisterUserRequestParser, SendMailChangeRequestParser, UpdateUserRequestParser}
import play.api.mvc._
import dev.pompilius.auth.infrastructure.parsers.MailTokenParser

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import dev.pompilius.attachment.infrastructure.writers.AttachmentWriter
import dev.pompilius.auth.domain.MailToken
import dev.pompilius.auth.infrastructure.writers.MailTokenWriter
import dev.pompilius.mail.domain._
import dev.pompilius.shared.infrastructure.UrlUtil
import play.api.i18n.MessagesImpl

@Singleton
class UserController @Inject() (
    attachmentCheck: AttachmentCheck,
    userWriter: UserWriter,
    userRepository: UserRepository,
    attachmentWriter: AttachmentWriter,
    mailTokenWriter: MailTokenWriter,
    sendMailQueue: SendMailQueue
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {

  private val logger = play.api.Logger(getClass)

  def registerUser: Action[AnyContent] =
    Action.async { implicit request =>
      val createUserRequest = RegisterUserRequestParser.parse(request)

      val newUser = User(
        id = UserId.gen(configuration.nodeId),
        username = createUserRequest.username,
        passwordHash = UserPassword(createUserRequest.password).hash,
        enabled = true,
        email = createUserRequest.email,
        country = createUserRequest.country,
        firstName = createUserRequest.firstName,
        lastName = createUserRequest.lastName,
        phone = createUserRequest.phone,
        language = createUserRequest.language,
        created = clock.now,
        updated = clock.now,
        avatar = None,
        notes = createUserRequest.notes,
        bio = createUserRequest.bio
      )

      for {
        _ <- userRepository.findByEmail(newUser.email).map {
          case Some(_) =>
            throw new EmailAlreadyInUseException()
          case _ =>
        }

        _ <- userRepository.findByUsername(newUser.username).map {
          case Some(_) =>
            throw new UsernameAlreadyInUseException()
          case _ =>
        }

        _ <- userRepository.save(newUser)

        newUserRole = UserRole(newUser.id, createUserRequest.role)
        _ <- userRoleRepository.save(newUserRole)

        newUserJson <- userWriter.toJson(newUser)
      } yield {
        Ok(newUserJson)
      }
    }

  def downloadAvatar(userId: String): Action[AnyContent] =
    Action.async {
      val pid = UserId(userId)
      for {
        user <-
          userRepository
            .findById(pid)
            .map(
              _.getOrElse(throw new UserNotFoundException(s"User with id $userId not found"))
            )

        attachmentId = user.avatar.getOrElse(throw new NotFoundException(s"Person with id $userId has no avatar"))

        result <- download(Some(user), attachmentId)

      } yield {
        result
      }
    }

  def updateUser(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val updateUserRequest = UpdateUserRequestParser.parse(request)

          for {

            _ <- userRepository.findByUsername(updateUserRequest.username).map {
              // Si otro usuario está usando este username, lanzamos una excepción
              case Some(u) if u.id.id != currentUser.id.id =>
                throw new UsernameAlreadyInUseException()
              case _ =>
            }

            updatedUser = currentUser.copy(
              username = updateUserRequest.username,
              phone = updateUserRequest.phone,
              firstName = updateUserRequest.firstName,
              lastName = updateUserRequest.lastName,
              country = updateUserRequest.country,
              language = updateUserRequest.language,
              bio = updateUserRequest.bio,
              updated = clock.now
            )

            _ <- userRepository.save(updatedUser)

            newUserJson <- userWriter.toJson(updatedUser)
          } yield {
            Ok(newUserJson)
          }
      }
    }

  def changePassword: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (currentSession, currentUser, _) =>
          val changeUserPasswordRequest = ChangeUserPasswordRequestParser.parse(request)

          if (!UserPassword(changeUserPasswordRequest.oldPassword).check(currentUser.passwordHash)) {
            throw new InvalidPasswordOrUsernameException("The old password does not match")
          }

          val updatedUser = currentUser.copy(
            passwordHash = UserPassword(changeUserPasswordRequest.newPassword).hash
          )

          for {
            _ <- userRepository.save(updatedUser)

            _ <-
              if (changeUserPasswordRequest.closeAllSessions) {
                sessionRepository.closeAllSessions(updatedUser.id, Some(currentSession.id))
              } else {
                Future.unit
              }

            newUserJson <- userWriter.toJson(updatedUser)
          } yield {
            Ok(newUserJson)
          }
      }
    }

  def uploadMyAvatar(): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            attachment <- uploadImage(
              user = user,
              body = request.body,
              maxWidth = configuration.attachments.avatars.maxWidth,
              maxHeight = configuration.attachments.avatars.maxHeight
            )
            _ <- userRepository.save(
              user.copy(
                updated = clock.now,
                avatar = Some(attachment.id)
              )
            )

            json <- attachmentWriter.asCurrentUser(attachment)
          } yield {
            Ok(json)
          }
      }
    }

  def changeAvatar(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val uploadedAttachmentRequest = UploadedAttachmentRequestParser.parse(request)

          for {
            _ <- attachmentCheck.check(uploadedAttachmentRequest.id)

            updatedUser = currentUser.copy(
              avatar = Some(uploadedAttachmentRequest.id)
            )

            _ <- userRepository.save(updatedUser)

            updatedUserJson <- userWriter.toJson(updatedUser)
          } yield {
            Ok(updatedUserJson)
          }
      }
    }

  def deleteAvatar(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val updatedUser = currentUser.copy(
            avatar = None
          )

          for {
            _ <- userRepository.save(updatedUser)

            updatedUserJson <- userWriter.toJson(updatedUser)
          } yield {
            Ok(updatedUserJson)
          }
      }
    }

  def deleteUser(): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val updatedUser = currentUser.copy(
            enabled = false,
            updated = clock.now
          )
          for {
            _ <- userRepository.save(updatedUser)
          } yield {
            Ok
          }
      }
    }

  def changeEmail: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val changeMailRequest = ChangeMailRequestParser.parse(request)
          val mailToken = MailTokenParser.parse(changeMailRequest.token, configuration.mails.tokenSecretKey)

          if (!mailToken.mail.equalsIgnoreCase(changeMailRequest.email)) {
            throw new BadRequestException("The email does not match with the token")
          }

          for {
            _ <- userRepository.findByEmail(mailToken.mail).map {
              // Si otro usuario esta usando este email, lanzamos una excepción
              case Some(u) if u.id.id != currentUser.id.id =>
                throw new EmailAlreadyInUseException()
              case _ =>
            }

            updatedUser = currentUser.copy(
              email = mailToken.mail
            )

            _ <- userRepository.save(updatedUser)

            newUserJson <- userWriter.toJson(updatedUser)
          } yield {
            Ok(newUserJson)
          }
      }

      def sendChangeMail: Action[AnyContent] =
        Action.async { implicit request =>
          withAuthenticatedUser {
            case (_, _, _) =>
              val sendMailChangeRequest = SendMailChangeRequestParser.parse(request)

              for {
                _ <-
                  mailTokenWriter
                    .toString(
                      MailToken(
                        sendMailChangeRequest.email,
                        clock.now.plusMillis(configuration.users.changeMailLinkDuration.toMillis.toInt)
                      ),
                      configuration.mails.tokenSecretKey
                    ) map { token =>
                    // Creamos un token y lo enviamos por email
                    val messages = MessagesImpl(getLanguage, messagesApi)
                    val mailAddress = MailAddress(address = sendMailChangeRequest.email, name = None)
                    // Creamos el link de autenticación, que ira en el correo.
                    val link = UrlUtil.addQueryParameters(
                      configuration.users.changeMailUrl,
                      Map(Strings.email -> sendMailChangeRequest.email, Strings.token -> token)
                    )
                    // Creamos el contenido del email de pago usando una plantilla
                    val mailContent =
                      dev.pompilius.users.infrastructure.views.html.change_email(link)(messages)

                    val mail = Mail(
                      to = mailAddress,
                      subject = Some(MailSubject(messages("mail.change.subject"))),
                      content = MailContent(
                        contentType = mailContent.contentType,
                        content = mailContent.body
                      )
                    )

                    // Enviamos el email
                    sendMailQueue.add(mail = mail, accountId = None, key = None)
                  }

              } yield {
                Ok
              }
          }
        }
    }

}
