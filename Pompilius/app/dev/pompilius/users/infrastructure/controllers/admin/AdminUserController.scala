//package dev.pompilius.users.infrastructure.controllers.admin
//
//import play.api.libs.Files.{TemporaryFile, TemporaryFileCreator}
//import play.api.libs.json.{JsObject, Json}
//import play.api.mvc.{Action, AnyContent, InjectedController}
//
//import java.io.FileOutputStream
//import javax.inject._
//import scala.concurrent.ExecutionContext
//
//@Singleton
//class AdminUserController @Inject()(
//    imageCheck: ImageCheck,
//    userWriter: UserWriter,
//    paginatedWriter: PaginatedWriter,
//    requestLogger: RequestLogger,
//    userRepository: UserRepository,
//    userRoleRepository: UserRoleRepository,
//    clock: Clock,
//    configuration: Configuration,
//    temporaryFileCreator: TemporaryFileCreator
//)(implicit ec: ExecutionContext)
//    extends InjectedController
//    with BaseController {
//
//  def createUser: Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS)) {
//        case (_, admin, _, _) =>
//          val adminCreateUserRequest = AdminCreateUserRequestParser.parse(request)
//
//          val newUser = User(
//            id = UserId.gen(configuration.nodeId),
//            username = adminCreateUserRequest.username,
//            // Si no se especifica password, se genera una aleatoria
//            passwordHash = adminCreateUserRequest.password
//              .map(UserPassword(_).hash)
//              .getOrElse(UserPassword(CipherUtil.randomCode(16)).hash),
//            enabled = adminCreateUserRequest.enabled,
//            level = 0,
//            email = adminCreateUserRequest.email,
//            phone = adminCreateUserRequest.phone,
//            avatar = None,
//            header = None,
//            firstName = adminCreateUserRequest.firstName,
//            lastName = adminCreateUserRequest.lastName,
//            country = adminCreateUserRequest.country,
//            language = adminCreateUserRequest.language,
//            notes = adminCreateUserRequest.notes,
//            bio = adminCreateUserRequest.bio,
//            notifyOnSecretReceived = true,
//            notifyOnFileReceived = true,
//            notifyOnSecretOpened = true,
//            notifyOnFileDownload = true,
//            created = clock.now,
//            updated = clock.now
//          )
//
//          for {
//            _ <- userRepository.findByEmail(newUser.email).map {
//              case Some(_) =>
//                throw new EmailAlreadyInUseException()
//              case _ =>
//            }
//
//            _ <- userRepository.findByUsername(newUser.username).map {
//              case Some(_) =>
//                throw new UsernameAlreadyInUseException()
//              case _ =>
//            }
//
//            _ <- userRepository.save(newUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.as[JsObject] - Strings.password).map(Json.stringify),
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toAdminJson(newUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def updateUser(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS)) {
//        case (_, admin, _, _) =>
//          val adminUpdateUserRequest = AdminUpdateUserRequestParser.parse(request)
//
//          for {
//            currentUser <- userRepository.getById(UserId(userId))
//
//            _ = checkLevels(admin, currentUser)
//
//            _ <- userRepository.findByEmail(adminUpdateUserRequest.email).map {
//              // Si otro usuario esta usando este email, lanzamos una excepción
//              case Some(u) if u.id.id != currentUser.id.id =>
//                throw new EmailAlreadyInUseException()
//              case _ =>
//            }
//
//            _ <- userRepository.findByUsername(adminUpdateUserRequest.username).map {
//              // Si otro usuario esta usando este username, lanzamos una excepción
//              case Some(u) if u.id.id != currentUser.id.id =>
//                throw new UsernameAlreadyInUseException()
//              case _ =>
//            }
//
//            updatedUser = currentUser.copy(
//              username = adminUpdateUserRequest.username,
//              enabled = adminUpdateUserRequest.enabled,
//              email = adminUpdateUserRequest.email,
//              phone = adminUpdateUserRequest.phone,
//              firstName = adminUpdateUserRequest.firstName,
//              lastName = adminUpdateUserRequest.lastName,
//              country = adminUpdateUserRequest.country,
//              language = adminUpdateUserRequest.language,
//              notes = adminUpdateUserRequest.notes,
//              bio = adminUpdateUserRequest.bio,
//              updated = clock.now
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toAdminJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def getUser(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS, Permission.VIEW_USERS)) {
//        case (_, _, _, _) =>
//          for {
//            user <- userRepository.getById(UserId(userId))
//            json <- userWriter.toAdminJson(user)
//          } yield {
//            Ok(json)
//          }
//      }
//    }
//
//  def getUsers(
//      username: Option[String],
//      firstName: Option[String],
//      lastName: Option[String],
//      enabled: Option[Boolean],
//      country: Option[String],
//      roleId: Option[String],
//      search: Option[String],
//      pag: Pagination
//  ): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS, Permission.VIEW_USERS)) {
//        case (_, _, _, _) =>
//          for {
//            users <- userRepository.find(
//              filter = UserFilter(
//                username = username,
//                firstName = firstName,
//                lastName = lastName,
//                enabled = enabled,
//                country = country.map(Country.withNameInsensitive),
//                roleId = roleId.map(RoleId(_)),
//                search = search
//              ),
//              pag = pag.oneMore
//            )
//
//            json <- paginatedWriter.toJson(Paginated(users, pag))(userWriter.toAdminJson)
//
//          } yield {
//            Ok(json)
//          }
//      }
//    }
//
//
//  def changeUserPassword(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS)) {
//        case (_, admin, _, _) =>
//          val adminChangeUserPasswordRequest = AdminChangeUserPasswordRequestParser.parse(request)
//
//          for {
//            currentUser <- userRepository.getById(UserId(userId))
//
//            _ = checkLevels(admin, currentUser)
//
//            updatedUser = currentUser.copy(
//              passwordHash = UserPassword(adminChangeUserPasswordRequest.password).hash
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = None,
//                metadata = None
//              )
//            )
//
//            newUserJson <- userWriter.toAdminJson(updatedUser)
//          } yield {
//            Ok(newUserJson)
//          }
//      }
//    }
//
//  def setUserRoles(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_ROLES)) {
//        case (_, admin, _, _) =>
//          val setUserRolesRequest = SetUserRolesRequestParser.parse(request)
//
//          for {
//            currentUser <- userRepository.getById(UserId(userId))
//
//            _ = checkLevels(admin, currentUser)
//
//            _ <- userRoleRepository.setUserRoles(
//              userId = currentUser.id,
//              roles = setUserRolesRequest.roles
//            )
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            currentUserJson <- userWriter.toAdminJson(currentUser)
//          } yield {
//            Ok(currentUserJson)
//          }
//      }
//    }
//
//  def changeAvatar(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS)) {
//        case (_, admin, _, _) =>
//          val uploadedImageRequest = UploadedImageRequestParser.parse(request)
//
//          for {
//            currentUser <- userRepository.getById(UserId(userId))
//
//            _ = checkLevels(admin, currentUser)
//
//            _ <- imageCheck.check(uploadedImageRequest.id)
//
//            updatedUser = currentUser.copy(
//              avatar = Some(uploadedImageRequest.id)
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            updatedUserJson <- userWriter.toAdminJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
//  def deleteAvatar(userId: String): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisPermissions(Seq(Permission.ADMIN_USERS)) {
//        case (_, admin, _, _) =>
//          for {
//            currentUser <- userRepository.getById(UserId(userId))
//
//            _ = checkLevels(admin, currentUser)
//
//            updatedUser = currentUser.copy(
//              avatar = None
//            )
//
//            _ <- userRepository.save(updatedUser)
//
//            _ <- requestLogger.log(
//              RequestLog(
//                id = RequestLogId.gen(config.nodeId),
//                userId = admin.id,
//                timestamp = clock.now,
//                address = request.remoteAddress,
//                method = request.method,
//                path = request.path,
//                body = request.body.asJson.map(_.toString),
//                metadata = None
//              )
//            )
//
//            updatedUserJson <- userWriter.toAdminJson(updatedUser)
//          } yield {
//            Ok(updatedUserJson)
//          }
//      }
//    }
//
