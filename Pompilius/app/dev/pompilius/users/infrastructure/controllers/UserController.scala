package dev.pompilius.users.infrastructure.controllers

import dev.pompilius.Strings
import dev.pompilius.attachment.domain.{AttachmentCheck, AttachmentRepository}
import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.attachment.infrastructure.parsers.UploadedAttachmentRequestParser
import dev.pompilius.auth.domain.SessionRepository
import dev.pompilius.auth.domain.exceptions.InvalidPasswordOrUsernameException
import dev.pompilius.country.domain.Country
import dev.pompilius.mail.domain._
import dev.pompilius.resource.domain.ResourceUserRepository
import dev.pompilius.shared.domain.exceptions.{BadRequestException, NotFoundException}
import dev.pompilius.shared.domain.{Paginated, Pagination}
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain._
import dev.pompilius.users.domain.exceptions.{
  EmailAlreadyInUseException,
  UserNotFoundException,
  UsernameAlreadyInUseException
}
import dev.pompilius.users.infrastructure.parsers.{
  ChangeUserPasswordRequestParser,
  RegisterUserRequestParser,
  UpdateUserRequestParser
}
import dev.pompilius.users.infrastructure.writers.UserWriter
import play.api.libs.Files
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject() (
    attachmentCheck: AttachmentCheck,
    userWriter: UserWriter,
    userRepository: UserRepository,
    mailRepository: MailRepository,
    resourceUserRepository: ResourceUserRepository,
    sessionRepository: SessionRepository,
    mailSentRepository: MailSentRepository,
    userAttachment: UserAttachmentRepository,
    attachmentRepository: AttachmentRepository,
    paginatedWriter: PaginatedWriter,
    userFollowerRepository: UserFollowersRepository
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {

  private val logger = play.api.Logger(getClass)

  def registerUser: Action[AnyContent] = {
    Action.async { implicit request =>
      val createUserRequest = RegisterUserRequestParser.parse(request)

      if (createUserRequest.role == Role.ADMIN) {
        throw new BadRequestException("Cannot register a user with ADMIN role")
      }

      val newUser = User(
        id = UserId.gen(configuration.nodeId),
        username = createUserRequest.username,
        passwordHash = UserPassword(createUserRequest.password).hash,
        enabled = true,
        email = createUserRequest.email,
        interests=null,
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
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, _, _) =>
          val pid = UserId(userId)
          for {
            user <-
              userRepository
                .findById(pid)
                .map(
                  _.getOrElse(throw new UserNotFoundException(s"User with id $userId not found"))
                )

            attachmentId = user.avatar.getOrElse(throw new NotFoundException(s"User with id $userId has no avatar"))

            result <- download(Some(user), attachmentId)

          } yield {
            result
          }
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
              interests = updateUserRequest.interests,
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
            updateUser = user.copy(
              updated = clock.now,
              avatar = Some(attachment.id)
            )
            _ <- userRepository.save(updateUser)

            _ <- userAttachment.save(UserAttachment(user.id, attachment.id))
            updatedUserJson <- userWriter.toJson(updateUser)
          } yield {
            Ok(updatedUserJson)
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
            avatar = None,
            updated = clock.now
          )

          for {
            avatarId <- Future.fromTry(
              currentUser.avatar
                .toRight(
                  new NotFoundException("User has no avatar")
                )
                .toTry
            )

            _ <- userAttachment.delete(currentUser.id, avatarId)
            _ <- attachmentRepository.delete(avatarId)

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
            _ <- sessionRepository.closeAllSessions(currentUser.id, None)
            _ <- resourceUserRepository.deleteAllResourceByUserId(currentUser.id)
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

  def searchUsers(
      search: Option[String],
      username: Option[String],
      firstName: Option[String],
      lastName: Option[String],
      country: Option[String],
      pag: Pagination
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, _, _) =>
          for {
            users <- userRepository.find(
              UserFilter(
                username = username,
                firstName = firstName,
                lastName = lastName,
                country = country.map(Country.withNameInsensitive),
                search = search,
                enabled = Some(true)
              ),
              pag.oneMore
            )
            json <- paginatedWriter.toJson(Paginated(users, pag))(user => userWriter.asAnotherUser(user = user))
          } yield {
            Ok(json)
          }
      }
    }

  def followUser(userId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val followedUserId = UserId(userId)
          for {
            _ <- userRepository.findById(followedUserId).map {
              case Some(_) =>
              case None =>
                throw new UserNotFoundException(s"User with id $userId not found")
            }
            _ <- userFollowerRepository.save(UserFollower(followedUserId,currentUser.id, clock.now))
          } yield {
            Ok
          }
      }
    }

def unfollowUser(userId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>
          val unfollowedUserId = UserId(userId)
          for {
            _ <- userRepository.findById(unfollowedUserId).map {
              case Some(_) =>
              case None =>
                throw new UserNotFoundException(s"User with id $userId not found")
            }
            _ <- userFollowerRepository.delete(UserFollower(unfollowedUserId, currentUser.id, clock.now))
          } yield {
            Ok
          }
      }
    }

  def getFollowers(userId: String, pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, _, _) =>
          val uid = UserId(userId)

          for {
            _ <- userRepository.findById(uid).map {
              case Some(_) =>
              case None =>
                throw new UserNotFoundException(s"User with id $uid not found")
            }

            followers <- userFollowerRepository.getAllByUserId(uid, pag.oneMore)
            followersJson <- Future.sequence(
              followers.map { follower =>
                userRepository.findById(follower.followerId).map {
                  case Some(user) => userWriter.asAnotherUser(user)
                  case None       => throw new UserNotFoundException(s"User with id ${follower.followerId} not found")
                }
              }
            )
            json <- paginatedWriter.toJson(Paginated(followersJson, pag))
          } yield {
            Ok(json)
          }
      }
    }

  def countFollowers(userId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, _, _) =>
          val uid = UserId(userId)
          for {
            _ <- userRepository.findById(uid).map {
              case Some(_) =>
              case None =>
                throw new UserNotFoundException(s"User with id $uid not found")
            }
            followersCount <- userFollowerRepository.countByUserId(uid)
          } yield {
            Ok(Json.obj(Strings.followersCounted -> followersCount))
          }
      }
    }
}

