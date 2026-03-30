package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.Strings.{description, resourceType, startDate}
import dev.pompilius.resource.domain.request.{CreateSampleRequest, CreateStudyRequest}
import dev.pompilius.resource.domain.sample.{Sample, SampleId, SampleRepository}
import dev.pompilius.resource.domain.study.{Study, StudyId, StudyRepository}
import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.resource.infrastructure.parsers.{CreateSampleRequestParser, CreateStudyRequestParser}
import dev.pompilius.resource.infrastructure.repositories.sample.SampleMySqlRepository
import dev.pompilius.resource.infrastructure.repositories.study.StudyMySqlRepository
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import dev.pompilius.users.domain.{Role, UserId}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResourceController @Inject() (
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    sampleRepository: SampleMySqlRepository,
    studyRepository: StudyMySqlRepository,
    resourceWriter: ResourceWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  def createSampleResource: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createSampleRequest = CreateSampleRequestParser.parse(request)

          // 1. Crear el Resource
          val newResource = Resource(
            id = ResourceId.gen(configuration.nodeId),
            resourceType = ResourceType.SAMPLE,
            visibility = createSampleRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createSampleRequest.localization,
            observations = createSampleRequest.observations,
            summary = createSampleRequest.summary
          )
          val newSampleResource = Sample(
            id = SampleId.gen(configuration.nodeId),
            resourceId = newResource.id,
            name = createSampleRequest.name,
            minerals = createSampleRequest.minerals,
            collectionMethods = createSampleRequest.collectionMethods,
            isFresh = createSampleRequest.isFresh,
            sampleType = createSampleRequest.sampleType,
            materialsUsed = createSampleRequest.materialsUsed,
            rockType = createSampleRequest.rockType,
            geologicalProcesses = createSampleRequest.geologicalProcesses
          )

          // 2. Guardar en BD
          for {
            _ <- resourceRepository.save(newResource)
            _ <- sampleRepository.save(newSampleResource)

            // Guardar la relación de propiedad
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = newResource.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            // Devolver el JSON con ambos datos, se creó un writer para cada uno
            json <- resourceWriter.asOwner(newResource, Some(newSampleResource), None)
          } yield {
            Ok(json)
          }
      }
    }

  def createStudyResource: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createStudyRequest = CreateStudyRequestParser.parse(request)

          // 1. Crear el Resource
          val newResource = Resource(
            id = ResourceId.gen(configuration.nodeId),
            resourceType = ResourceType.STUDY,
            visibility = createStudyRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createStudyRequest.localization,
            observations = createStudyRequest.observations,
            summary = createStudyRequest.summary
          )
          val newStudyResource = Study(
            id = StudyId.gen(configuration.nodeId),
            resourceId = newResource.id,
            name = createStudyRequest.name,
            startDate = createStudyRequest.startDate,
            endDate = createStudyRequest.endDate,
            description = createStudyRequest.description,
            coordinates = createStudyRequest.coordinates,
            area = createStudyRequest.area,
            methods = createStudyRequest.methods,
            authors = createStudyRequest.authors,
            section = createStudyRequest.section,
            antecedents = createStudyRequest.antecedents,
            nameSection = createStudyRequest.nameSection
          )

          // 2. Guardar en BD
          for {
            _ <- resourceRepository.save(newResource)
            _ <- studyRepository.save(newStudyResource)

            // Guardar la relación de propiedad
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = newResource.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            // Devolver el JSON con ambos datos
            json <- resourceWriter.asOwner(newResource, None, Some(newStudyResource))
          } yield {
            Ok(json)
          }
      }
    }

//  /**
//   * Lista todos los recursos con acceso para el usuario actual
//   * GET /api/resources?resourceType=...&visibility=...&page=...&pageSize=...
//   */
//  def listResources: Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        val resourceType = request.queryString.get("resourceType").flatMap(_.headOption)
//          .flatMap(rt => scala.util.Try(ResourceType.withNameInsensitive(rt)).toOption)
//        val visibility = request.queryString.get("visibility").flatMap(_.headOption)
//          .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)
//        val localization = request.queryString.get("localization").flatMap(_.headOption)
//        val page = request.queryString.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(1)
//        val pageSize = request.queryString.get("pageSize").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(10)
//
//        val filter = ResourceFilter(
//          resourceType = resourceType,
//          visibility = visibility,
//          localization = localization
//        )
//
//        for {
//          userResources <- resourceUserRepository.getAllByUserId(user.id)
//          allResources <- resourceRepository.find(filter, Pagination(page, pageSize))
//          userResourceIds = userResources.map(_.resourceId).toSet
//          resourceIds = userResourceIds ++ allResources.data.filter(_.visibility == Visibility.PUBLIC).map(_.id)
//          resources <- Future.sequence(
//            resourceIds.map { resId =>
//              for {
//                resourceOpt <- resourceRepository.findById(resId)
//                (sampleOpt, studyOpt) <- resourceOpt match {
//                  case Some(res) =>
//                    res.resourceType match {
//                      case ResourceType.SAMPLE =>
//                        sampleRepository.findByResourceId(resId).map(s => (s, None))
//                      case ResourceType.STUDY =>
//                        studyRepository.findByResourceId(resId).map(s => (None, s))
//                    }
//                  case None => Future.successful((None, None))
//                }
//              } yield (resourceOpt, sampleOpt, studyOpt)
//            }
//          ).map(_.flatten)
//          jsonResources <- Future.sequence(
//            resources.map { case (resource, sample, study) =>
//              resourceWriter.asPublic(resource, sample, study)
//            }
//          )
//        } yield {
//          Ok(Json.obj(
//            "data" -> jsonResources,
//            "page" -> page,
//            "pageSize" -> pageSize,
//            "total" -> resources.length
//          ))
//        }
//    }
//  }
//
//  /**
//   * Obtiene un recurso específico por ID
//   * GET /api/resources/:resourceId
//   */
//  def getResource(resourceId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          _ <- Future.successful(
//            if (resource.deleted) throw NotFoundException("Resource not found")
//          )
//          _ <- validateResourceAccess(resource, user.id)
//          (sampleOpt, studyOpt) <- resource.resourceType match {
//            case ResourceType.SAMPLE =>
//              sampleRepository.findByResourceId(resource.id).map(s => (s, None))
//            case ResourceType.STUDY =>
//              studyRepository.findByResourceId(resource.id).map(s => (None, s))
//          }
//          isOwner <- isResourceOwner(resource.id, user.id)
//          json <- if (isOwner) {
//            resourceWriter.asOwner(resource, sampleOpt, studyOpt)
//          } else {
//            resourceWriter.asPublic(resource, sampleOpt, studyOpt)
//          }
//        } yield {
//          Ok(json)
//        }
//    }
//  }
//
//  /**
//   * Actualiza un recurso existente
//   * PUT /api/resources/:resourceId
//   */
//  def updateResource(resourceId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))
//
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          _ <- isResourceOwnerOrThrow(resource.id, user.id)
//          observations = (body \ "observations").asOpt[String]
//          summary = (body \ "summary").asOpt[String]
//          visibility = (body \ "visibility").asOpt[String]
//            .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)
//          updatedResource = resource.copy(
//            observations = observations.orElse(resource.observations),
//            summary = summary.orElse(resource.summary),
//            visibility = visibility.getOrElse(resource.visibility),
//            updated = clock.now
//          )
//          _ <- resourceRepository.save(updatedResource)
//          (sampleOpt, studyOpt) <- updatedResource.resourceType match {
//            case ResourceType.SAMPLE =>
//              sampleRepository.findByResourceId(updatedResource.id).map(s => (s, None))
//            case ResourceType.STUDY =>
//              studyRepository.findByResourceId(updatedResource.id).map(s => (None, s))
//          }
//          json <- resourceWriter.asOwner(updatedResource, sampleOpt, studyOpt)
//        } yield {
//          Ok(json)
//        }
//    }
//  }
//
//  /**
//   * Borra un recurso (soft delete)
//   * DELETE /api/resources/:resourceId
//   */
//  def deleteResource(resourceId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          _ <- isResourceOwnerOrThrow(resource.id, user.id)
//          deletedResource = resource.copy(deleted = true, updated = clock.now)
//          _ <- resourceRepository.save(deletedResource)
//        } yield {
//          NoContent
//        }
//    }
//  }
//
//  /**
//   * Obtiene todos los recursos del usuario actual
//   * GET /api/resources/user/my-resources
//   */
//  def getMyResources: Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        for {
//          userResources <- resourceUserRepository.getAllByUserId(user.id)
//          resourceIds = userResources.map(_.resourceId)
//          resources <- Future.sequence(
//            resourceIds.map { resId =>
//              for {
//                resourceOpt <- resourceRepository.findById(resId)
//                (sampleOpt, studyOpt) <- resourceOpt match {
//                  case Some(res) =>
//                    res.resourceType match {
//                      case ResourceType.SAMPLE =>
//                        sampleRepository.findByResourceId(resId).map(s => (s, None))
//                      case ResourceType.STUDY =>
//                        studyRepository.findByResourceId(resId).map(s => (None, s))
//                    }
//                  case None => Future.successful((None, None))
//                }
//              } yield (resourceOpt, sampleOpt, studyOpt)
//            }
//          ).map(_.flatten)
//          jsonResources <- Future.sequence(
//            resources.map { case (resource, sample, study) =>
//              resourceWriter.asOwner(resource, sample, study)
//            }
//          )
//        } yield {
//          Ok(Json.arr(jsonResources: _*))
//        }
//    }
//  }
//
//  /**
//   * Compra un recurso
//   * POST /api/resources/:resourceId/purchase
//   */
//  def purchaseResource(resourceId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          isOwner <- isResourceOwner(resource.id, user.id)
//          _ <- Future.successful(
//            if (isOwner) {
//              throw BadRequestException("Cannot purchase your own resource")
//            }
//          )
//          _ <- resourceUserRepository.save(
//            ResourceUser(
//              resourceId = resource.id,
//              userId = user.id,
//              resourceUserType = ResourceUserType.BOUGHT,
//              created = clock.now
//            )
//          )
//          json <- Future.successful(Json.obj(
//            "resourceId" -> resource.id.toString,
//            "userId" -> user.id.toString,
//            "resourceUserType" -> ResourceUserType.BOUGHT.toString,
//            "created" -> clock.now.toString
//          ))
//        } yield {
//          Created(json)
//        }
//    }
//  }
//
//  /**
//   * Ofrece un recurso como forma de pago (intercambio/trueque)
//   * POST /api/resources/:resourceId/offer-as-payment/:toUserId
//   */
//  def offerResourceAsPayment(resourceId: Long, toUserId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          _ <- isResourceOwnerOrThrow(resource.id, user.id)
//          _ <- resourceUserRepository.save(
//            ResourceUser(
//              resourceId = resource.id,
//              userId = UserId(toUserId),
//              resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
//              created = clock.now
//            )
//          )
//          json <- Future.successful(Json.obj(
//            "resourceId" -> resource.id.toString,
//            "userId" -> UserId(toUserId).toString,
//            "resourceUserType" -> ResourceUserType.ACCEPTED_AS_PAYMENT.toString,
//            "created" -> clock.now.toString
//          ))
//        } yield {
//          Created(json)
//        }
//    }
//  }
//
//  /**
//   * Otorga acceso genérico a un recurso
//   * POST /api/resources/:resourceId/grant-access
//   * Body: { "userId": 5, "accessType": "BOUGHT" | "ACCEPTED_AS_PAYMENT" }
//   */
//  def grantResourceAccess(resourceId: Long): Action[AnyContent] = Action.async { implicit request =>
//    withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//      case (_, user, _, _) =>
//        val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))
//
//        val targetUserId = (body \ "userId").asOpt[Long]
//          .getOrElse(throw BadRequestException("Missing userId"))
//
//        val accessType = (body \ "accessType").asOpt[String]
//          .flatMap(at => scala.util.Try(ResourceUserType.withNameInsensitive(at)).toOption)
//          .getOrElse(throw BadRequestException("Invalid accessType"))
//
//        if (accessType == ResourceUserType.OWNER) {
//          throw BadRequestException("Cannot grant OWNER access through this endpoint")
//        }
//
//        for {
//          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//          _ <- isResourceOwnerOrThrow(resource.id, user.id)
//          _ <- resourceUserRepository.save(
//            ResourceUser(
//              resourceId = resource.id,
//              userId = UserId(targetUserId),
//              resourceUserType = accessType,
//              created = clock.now
//            )
//          )
//          json <- Future.successful(Json.obj(
//            "resourceId" -> resource.id.toString,
//            "userId" -> UserId(targetUserId).toString,
//            "resourceUserType" -> accessType.toString,
//            "created" -> clock.now.toString
//          ))
//        } yield {
//          Created(json)
//        }
//    }
//  }
//
//  // ===== MÉTODOS PRIVADOS =====
//
//  private def validateResourceAccess(resource: Resource, userId: UserId): Future[Unit] = {
//    resource.visibility match {
//      case Visibility.PUBLIC =>
//        Future.successful(())
//
//      case Visibility.PRIVATE =>
//        isResourceOwner(resource.id, userId).map {
//          case true => ()
//          case false => throw ForbiddenException("This resource is private")
//        }
//
//      case _ =>
//        resourceUserRepository.findByResourceAndUser(resource.id, userId).map {
//          case Some(_) => ()
//          case None => throw ForbiddenException("You don't have access to this resource")
//        }
//    }
//  }
//
//  private def isResourceOwner(resourceId: ResourceId, userId: UserId): Future[Boolean] = {
//    resourceUserRepository.findByResourceAndUser(resourceId, userId).map {
//      case Some(ru) => ru.resourceUserType == ResourceUserType.OWNER
//      case None => false
//    }
//  }
//
//  private def isResourceOwnerOrThrow(resourceId: ResourceId, userId: UserId): Future[Unit] = {
//    isResourceOwner(resourceId, userId).map {
//      case true => ()
//      case false => throw ForbiddenException("Only the resource owner can perform this action")
//    }
//  }
//}
////    Action.async { implicit request =>
////      val userId = getCurrentUserId
////
////      val resourceType = request.queryString.get("resourceType").flatMap(_.headOption)
////        .flatMap(rt => scala.util.Try(ResourceType.withNameInsensitive(rt)).toOption)
////      val visibility = request.queryString.get("visibility").flatMap(_.headOption)
////        .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)
////      val localization = request.queryString.get("localization").flatMap(_.headOption)
////      val page = request.queryString.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(1)
////      val pageSize = request.queryString.get("pageSize").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(10)
////
////      val filter = ResourceFilter(
////        resourceType = resourceType,
////        visibility = visibility,
////        localization = localization,
////        deleted = Some(false)
////      )
////
////      val pagination = Pagination(page = page, pageSize = pageSize)
////
////      for {
////        // 1. Obtener todos los recursos del usuario
////        userResources <- resourceUserRepository.getAllByUserId(UserId(userId))
////
////        // 2. Obtener todos los recursos públicos no borrados
////        allResources <- resourceRepository.find(filter, pagination)
////
////        // 3. Combinar: recursos del usuario + recursos públicos (sin duplicados)
////        userResourceIds = userResources.map(_.resourceId.id).toSet
////        resourceIds = userResourceIds ++ allResources.filter(_.visibility == Visibility.PUBLIC).map(_.id.id)
////
////        // 4. Obtener los recursos finales
////        resources <- Future.sequence(
////          resourceIds.map(id => resourceRepository.findById(ResourceId(id)))
////        ).map(_.flatten.toList)
////
////        paginatedResources = Paginated(data = resources, page = page, pageSize = pageSize, total = resources.length)
////        json <- paginatedWriter.toJson(paginatedResources)
////      } yield {
////        Ok(json)
////      }
////    }
////  }
////
////  /**
////   * Obtiene un recurso específico por ID
////   * GET /api/resources/:resourceId
////   */
////  def getResource(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que no está borrado
////          _ <- Future.successful(
////            if (resource.deleted) throw NotFoundException(s"Resource not found")
////          )
////
////          // Validar acceso
////          _ <- validateResourceAccess(resource, userId)
////
////          json <- toJson(resource)
////        } yield {
////          Ok(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error getting resource: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Actualiza un recurso existente
////   * PUT /api/resources/:resourceId
////   */
////  def updateResource(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////        val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que el usuario es el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id != userId) {
////              throw ForbiddenException("Only the resource owner can update this resource")
////            }
////          )
////
////          observations = (body \ "observations").asOpt[String]
////          summary = (body \ "summary").asOpt[String]
////          visibility = (body \ "visibility").asOpt[String]
////            .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)
////
////          updatedResource = resource.copy(
////            observations = observations.orElse(resource.observations),
////            summary = summary.orElse(resource.summary),
////            visibility = visibility.getOrElse(resource.visibility),
////            updated = clock.now
////          )
////
////          _ <- resourceRepository.save(updatedResource)
////          json <- toJson(updatedResource)
////        } yield {
////          Ok(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error updating resource: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Borra un recurso (soft delete)
////   * DELETE /api/resources/:resourceId
////   */
////  def deleteResource(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que el usuario es el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id != userId) {
////              throw ForbiddenException("Only the resource owner can delete this resource")
////            }
////          )
////
////          // Soft delete: marcar como deleted = true
////          deletedResource = resource.copy(deleted = true)
////          _ <- resourceRepository.save(deletedResource)
////        } yield {
////          NoContent
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error deleting resource: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Obtiene los usuarios con acceso a un recurso
////   * GET /api/resources/:resourceId/users
////   */
////  def getResourceUsers(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que es el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id != userId) {
////              throw ForbiddenException("Only the resource owner can view users")
////            }
////          )
////
////          users <- resourceUserRepository.getAllByResourceId(ResourceId(resourceId))
////          json <- toJsonResourceUsers(users)
////        } yield {
////          Ok(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error getting resource users: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Obtiene todos los recursos del usuario actual
////   * GET /api/resources/user/my-resources
////   */
////  def getMyResources: Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          userResources <- resourceUserRepository.getAllByUserId(UserId(userId))
////          resources <- Future.sequence(
////            userResources.map(ru => resourceRepository.findById(ru.resourceId))
////          ).map(_.flatten)
////
////          json <- toJsonResources(resources)
////        } yield {
////          Ok(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error getting my resources: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Compra un recurso
////   * POST /api/resources/:resourceId/purchase
////   */
////  def purchaseResource(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que no sea el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id == userId) {
////              throw BadRequestException("Cannot purchase your own resource")
////            }
////          )
////
////          // Crear acceso como BOUGHT
////          _ <- resourceUserRepository.save(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(userId),
////              resourceUserType = ResourceUserType.BOUGHT,
////              grantedAt = clock.now
////            )
////          )
////
////          json <- toJsonResourceUser(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(userId),
////              resourceUserType = ResourceUserType.BOUGHT,
////              grantedAt = clock.now
////            )
////          )
////        } yield {
////          Created(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error purchasing resource: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Ofrece un recurso como forma de pago (intercambio/trueque)
////   * POST /api/resources/:resourceId/offer-as-payment/:toUserId
////   */
////  def offerResourceAsPayment(resourceId: Long, toUserId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que el usuario es el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id != userId) {
////              throw ForbiddenException("Only the resource owner can offer resources")
////            }
////          )
////
////          // Otorgar acceso como ACCEPTED_AS_PAYMENT
////          _ <- resourceUserRepository.save(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(toUserId),
////              resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
////              grantedAt = clock.now
////            )
////          )
////
////          json <- toJsonResourceUser(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(toUserId),
////              resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
////              grantedAt = clock.now
////            )
////          )
////        } yield {
////          Created(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error offering resource as payment: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Otorga acceso genérico a un recurso
////   * POST /api/resources/:resourceId/grant-access
////   * Body: { "userId": 5, "accessType": "BOUGHT" | "ACCEPTED_AS_PAYMENT" }
////   */
////  def grantResourceAccess(resourceId: Long): Action[AnyContent] = {
////    Action.async { implicit request =>
////      try {
////        val userId = getCurrentUserId
////        val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))
////
////        val targetUserId = (body \ "userId").asOpt[Long]
////          .getOrElse(throw BadRequestException("Missing userId"))
////
////        val accessType = (body \ "accessType").asOpt[String]
////          .flatMap(at => scala.util.Try(ResourceUserType.withNameInsensitive(at)).toOption)
////          .getOrElse(throw BadRequestException("Invalid accessType"))
////
////        // Validar que sea BOUGHT o ACCEPTED_AS_PAYMENT (no OWNER)
////        if (accessType == ResourceUserType.OWNER) {
////          throw BadRequestException("Cannot grant OWNER access through this endpoint")
////        }
////
////        for {
////          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
////          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource not found")))
////
////          // Validar que el usuario es el propietario
////          _ <- Future.successful(
////            if (resource.ownerId.id != userId) {
////              throw ForbiddenException("Only the resource owner can grant access")
////            }
////          )
////
////          // Crear o actualizar el acceso en ResourceUser
////          _ <- resourceUserRepository.save(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(targetUserId),
////              resourceUserType = accessType,
////              grantedAt = clock.now
////            )
////          )
////
////          json <- toJsonResourceUser(
////            ResourceUser(
////              resourceId = ResourceId(resourceId),
////              userId = UserId(targetUserId),
////              resourceUserType = accessType,
////              grantedAt = clock.now
////            )
////          )
////        } yield {
////          Created(json)
////        }
////      } catch {
////        case e: Exception =>
////          logger.error(s"Error granting resource access: ${e.getMessage}")
////          Future.successful(BadRequest(Json.obj("error" -> e.getMessage)))
////      }
////    }
////  }
////
////  /**
////   * Valida si un usuario tiene acceso a un recurso
////   */
////  private def validateResourceAccess(resource: Resource, userId: Long): Future[Unit] = {
////    resource.visibility match {
////      case Visibility.PUBLIC =>
////        Future.successful(())
////
////      case Visibility.PRIVATE =>
////        if (resource.ownerId.id == userId) {
////          Future.successful(())
////        } else {
////          throw ForbiddenException("This resource is private")
////        }
////
////      case _ =>
////        resourceUserRepository.findBy(resource.id, UserId(userId)).map {
////          case Some(_) => ()
////          case None => throw ForbiddenException("You don't have access to this resource")
////        }
////    }
////  }
////
////  private def toJson(resource: Resource): Future[JsValue] = {
////    Future.successful(
////      Json.obj(
////        "id" -> resource.id.id.toString,
////        "resourceType" -> resource.resourceType.toString,
////        "ownerId" -> resource.ownerId.id.toString,
////        "deleted" -> resource.deleted,
////        "visibility" -> resource.visibility.toString,
////        "created" -> resource.created.toString,
////        "updated" -> resource.updated.toString,
////        "observations" -> resource.observations,
////        "summary" -> resource.summary,
////        "localization" -> resource.localization
////      )
////    )
////  }
////
////  private def toJsonResourceUser(resourceUser: ResourceUser): Future[JsValue] = {
////    Future.successful(
////      Json.obj(
////        "resourceId" -> resourceUser.resourceId.id.toString,
////        "userId" -> resourceUser.userId.id.toString,
////        "resourceUserType" -> resourceUser.resourceUserType.toString,
////        "grantedAt" -> resourceUser.grantedAt.toString
////      )
////    )
////  }
////
////  private def toJsonResourceUsers(users: List[ResourceUser]): Future[JsValue] = {
////    Future.successful(
////      Json.toJson(users.map(u =>
////        Json.obj(
////          "resourceId" -> u.resourceId.id.toString,
////          "userId" -> u.userId.id.toString,
////          "resourceUserType" -> u.resourceUserType.toString,
////          "grantedAt" -> u.grantedAt.toString
////        )
////      ))
////    )
////  }
////
////  private def toJsonResources(resources: List[Resource]): Future[JsValue] = {
////    Future.successful(
////      Json.toJson(resources.map(r =>
////        Json.obj(
////          "id" -> r.id.id.toString,
////          "resourceType" -> r.resourceType.toString,
////          "ownerId" -> r.ownerId.id.toString,
////          "deleted" -> r.deleted,
////          "visibility" -> r.visibility.toString,
////          "created" -> r.created.toString,
////          "updated" -> r.updated.toString
////        )
////      ))
////    )
////  }
////
////  private def getCurrentUserId: Long = {
////    getAuthenticatedUser.id.id
////  }
////}

}
