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
import play.api.libs.json.{JsValue, Json}
import dev.pompilius.users.domain.exceptions.{
  EmailAlreadyInUseException,
  UserNotFoundException,
  UsernameAlreadyInUseException
}
import dev.pompilius.users.infrastructure.parsers.{
  ChangeMailRequestParser,
  ChangeUserPasswordRequestParser,
  RegisterUserRequestParser,
  SendMailChangeRequestParser,
  UpdateUserRequestParser
}
import play.api.mvc._
import dev.pompilius.auth.infrastructure.parsers.MailTokenParser

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import dev.pompilius.attachment.infrastructure.writers.AttachmentWriter
import dev.pompilius.auth.domain.MailToken
import dev.pompilius.auth.infrastructure.writers.MailTokenWriter
import dev.pompilius.mail.domain._
import dev.pompilius.shared.infrastructure.UrlUtil
import dev.pompilius.users.domain.Role.{AMATEUR, PROFESSIONAL}
import play.api.i18n.MessagesImpl

@Singleton
class UserController @Inject() (
    attachmentCheck: AttachmentCheck,
    userWriter: UserWriter,
    userRepository: UserRepository,
    attachmentWriter: AttachmentWriter,
    mailTokenWriter: MailTokenWriter,
    mailRepository: MailRepository,
    mailSentRepository: MailSentRepository
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {

  private val logger = play.api.Logger(getClass)

  def registerUser: Action[AnyContent] = {
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
        _ <- sendWelcomeEmail(newUser)

        newUserRole = UserRole(newUser.id, createUserRequest.role)
        _ <- userRoleRepository.save(newUserRole)

        newUserJson <- userWriter.toJson(newUser)
      } yield {
        Ok(newUserJson)
      }
    }
  }

  private def sendWelcomeEmail(user: User): Future[Unit] = {
    val address = MailAddress(name = Some(user.fullName), address = user.email)
    val html = dev.pompilius.users.infrastructure.views.html
      .welcome_email(
        name = user.firstName.getOrElse(user.username),
        actionUrl = configuration.baseUrl
      )
      .body

    val content = MailContent(text = None, html = Some(html))
    val mail = Mail(
      to = address,
      subject = Some(MailSubject("Welcome to Our Research Community")),
      content = content
    )

    for {
      _ <- mailRepository.sendMail(mail)
      _ <- mailSentRepository.save(
        MailSent(
          id = MailSentId.gen(configuration.nodeId),
          mailType = MailType.WELCOME,
          address = address.address,
          sentAt = clock.now,
          userId = user.id,
          metadata = None
        )
      )
    } yield ()
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


  //Otra función generada para enviar emails, vamos a intentar y si no comentamos

  //Para validar el formato del username y su disponibilidad, esto es para mejorar la experiencia de usaurio en el frontend,
  // no es estrictamente necesario ya que el backend también validará esto al crear o actualizar un usuario, pero así evitamos que el usuario
  // tenga que esperar a enviar el formulario para saber si el username es válido o no.
  def validateUsername: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      (request.body \ Strings.username).as[String] match {
        case Strings.usernameRegex(username) =>
          userRepository.findByUsername(username).map(_.isEmpty).map { available =>
            // Si el nombre de usuario es válido, devolvemos su disponibilidad
            Ok(Json.obj(Strings.available -> available, Strings.valid -> true))
          }
        case _ =>
          // Si el nombre de usuario no es válido, devolvemos un error
          Future.successful(Ok(Json.obj(Strings.available -> false, Strings.valid -> false)))
      }
    }

  def validateEmail: Action[JsValue] =
    Action.async(parse.json) { implicit request =>
      (request.body \ Strings.email).as[String] match {
        case Strings.emailRegex(email) =>
          userRepository.findByEmail(email).map(_.isEmpty).map { available =>
            // Si el email es válido, se devuelve su disponibilidad
            Ok(Json.obj(Strings.available -> available, Strings.valid -> true))
          }
        case _ =>
          // Si el email no es válido, se devuelve un error
          Future.successful(Ok(Json.obj(Strings.available -> false, Strings.valid -> false)))
      }
    }
}
