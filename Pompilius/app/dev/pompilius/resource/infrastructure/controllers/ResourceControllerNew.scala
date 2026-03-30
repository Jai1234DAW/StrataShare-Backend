package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.sample.{Sample, SampleRepository}
import dev.pompilius.resource.domain.study.{Study, StudyRepository}
import dev.pompilius.resource.domain.{
  Resource,
  ResourceId,
  ResourceRepository,
  ResourceType,
  ResourceUser,
  ResourceUserRepository,
  ResourceUserType
}
import dev.pompilius.resource.infrastructure.repositories.sample.SampleMySqlRepository
import dev.pompilius.resource.infrastructure.repositories.study.StudyMySqlRepository
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Controller para operaciones generales de Resource
  * Maneja operaciones que funcionan en cualquier tipo de recurso (Sample, Study, etc.)
  * - Listar todos los recursos
  * - Obtener por ID (auto-detecta tipo)
  * - Comprar/acceder
  * - Otorgar acceso
  * - Eliminar
  */

@Singleton
class ResourceController @Inject() (
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    sampleRepository: SampleRepository,
    studyRepository: StudyRepository,
    resourceWriter: ResourceWriter,
    paginatedWriter: PaginatedWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  /**
    * Lista todos los recursos del usuario autenticado (Samples + Studies)
    * GET /api/resources
    */
  def list(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val userSamplesF: Future[Seq[Sample]] = sampleRepository.findAllByUser(user.id)
          val userStudiesF: Future[Seq[Study]] = studyRepository.findAllByUser(user.id)

          val sampleJsonsF: Future[Seq[JsValue]] = userSamplesF.flatMap { userSamples =>
            Future
              .sequence(
                userSamples.map { sample =>
                  resourceRepository.findById(sample.resourceId).flatMap {
                    case Some(resource) if !resource.deleted =>
                      resourceWriter.asOwner(resource, Some(sample), None)
                    case _ =>
                      Future.successful(Json.obj())
                  }
                }
              )
              .map(_.filter(_ != Json.obj())) // Filtrar JSON vacíos
          }

          val studyJsonsF: Future[Seq[JsValue]] = userStudiesF.flatMap { userStudies =>
            Future
              .sequence(
                userStudies.map { study =>
                  resourceRepository.findById(study.resourceId).flatMap {
                    case Some(resource) if !resource.deleted =>
                      resourceWriter.asOwner(resource, None, Some(study))
                    case _ =>
                      Future.successful(Json.obj())
                  }
                }
              )
              .map(_.filter(_ != Json.obj())) // Filtrar JSON vacíos
          }

          for {
            sampleJsons <- sampleJsonsF
            studyJsons <- studyJsonsF
            allResources = (sampleJsons ++ studyJsons).toList

            json <- paginatedWriter.toJson(Paginated(allResources, pag)) { (resource: JsValue) =>
              Future.successful(resource)
            }
          } yield Ok(json)
      }
    }
}

//  /**
//   * Obtiene un recurso específico por ID (auto-detecta si es Sample o Study)
//   * GET /api/resources/:resourceId
//   */
//  def get(resourceId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Validar que no está borrado
//            _ <- Future.successful(
//              if (resource.deleted) throw NotFoundException("Resource not found")
//            )
//
//            // Validar acceso
//            _ <- validateResourceAccess(resource, user.id)
//
//            // Obtener el Sample o Study según el tipo
//            (sampleOpt, studyOpt) <- resource.resourceType match {
//              case ResourceType.SAMPLE =>
//                sampleRepository.findByResourceId(resource.id).map(s => (s, None))
//              case ResourceType.STUDY =>
//                studyRepository.findByResourceId(resource.id).map(s => (None, s))
//            }
//
//            // Determinar si es propietario
//            isOwner <- isResourceOwner(resource.id, user.id)
//
//            // Generar JSON
//            json <- if (isOwner) {
//              resourceWriter.asOwner(resource, sampleOpt, studyOpt)
//            } else {
//              resourceWriter.asPublic(resource, sampleOpt, studyOpt)
//            }
//          } yield {
//            Ok(json)
//          }
//      }
//    }
//
//  /**
//   * Obtiene los recursos del usuario autenticado (alias de list sin paginación)
//   * GET /api/resources/my
//   */
//  def getMyResources: Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener todos los Samples del usuario
//            userSamples <- sampleRepository.findByUserId(user.id)
//
//            // Obtener todos los Studies del usuario
//            userStudies <- studyRepository.findByUserId(user.id)
//
//            // Convertir Samples a JSON
//            sampleJsons <- Future.sequence(
//              userSamples.map { sample =>
//                for {
//                  resourceOpt <- resourceRepository.findById(sample.resourceId)
//                  json <- resourceOpt match {
//                    case Some(resource) if !resource.deleted =>
//                      resourceWriter.asOwner(resource, Some(sample), None)
//                    case _ => Future.successful(Json.obj())
//                  }
//                } yield json
//              }
//            ).map(_.filter(_ != Json.obj()))
//
//            // Convertir Studies a JSON
//            studyJsons <- Future.sequence(
//              userStudies.map { study =>
//                for {
//                  resourceOpt <- resourceRepository.findById(study.resourceId)
//                  json <- resourceOpt match {
//                    case Some(resource) if !resource.deleted =>
//                      resourceWriter.asOwner(resource, None, Some(study))
//                    case _ => Future.successful(Json.obj())
//                  }
//                } yield json
//              }
//            ).map(_.filter(_ != Json.obj()))
//
//            // Combinar ambas listas
//            allResources = sampleJsons ++ studyJsons
//          } yield {
//            Ok(Json.obj(
//              "data" -> allResources,
//              "total" -> allResources.length
//            ))
//          }
//      }
//    }
//
//  /**
//   * Borra un recurso (soft delete)
//   * DELETE /api/resources/:resourceId
//   */
//  def delete(resourceId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Validar que es propietario
//            _ <- isResourceOwnerOrThrow(resource.id, user.id)
//
//            // Soft delete
//            deletedResource = resource.copy(deleted = true, updated = clock.now)
//            _ <- resourceRepository.save(deletedResource)
//          } yield {
//            NoContent
//          }
//      }
//    }
//
//  /**
//   * Obtiene los usuarios con acceso a un recurso
//   * GET /api/resources/:resourceId/users
//   */
//  def getResourceUsers(resourceId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Validar que es propietario
//            _ <- isResourceOwnerOrThrow(resource.id, user.id)
//
//            // Obtener usuarios con acceso
//            resourceUsers <- resourceUserRepository.getAllByResourceId(resource.id)
//
//            // Convertir a JSON
//            json <- Future.successful(
//              Json.obj(
//                "resourceId" -> resource.id.toString,
//                "users" -> resourceUsers.map(ru =>
//                  Json.obj(
//                    "userId" -> ru.userId.toString,
//                    "accessType" -> ru.resourceUserType.toString,
//                    "grantedAt" -> ru.created.toString
//                  )
//                )
//              )
//            )
//          } yield {
//            Ok(json)
//          }
//      }
//    }
//
//  /**
//   * Compra un recurso
//   * POST /api/resources/:resourceId/purchase
//   */
//  def purchase(resourceId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Obtener acceso actual del usuario al recurso
//            currentAccessOpt <- resourceUserRepository.findByResourceAndUser(resource.id, user.id)
//
//            // Validar según el acceso actual
//            _ <- Future.successful(
//              currentAccessOpt match {
//                case Some(access) =>
//                  access.resourceUserType match {
//                    case ResourceUserType.OWNER =>
//                      throw BadRequestException("Cannot purchase your own resource")
//                    case ResourceUserType.BOUGHT =>
//                      throw BadRequestException("You already own access to this resource")
//                    case ResourceUserType.ACCEPTED_AS_PAYMENT =>
//                      throw BadRequestException("You already have access to this resource through payment")
//                    case _ =>
//                      throw BadRequestException("You already have access to this resource")
//                  }
//                case None => () // Sin acceso, puede comprar
//              }
//            )
//
//            // Crear acceso como BOUGHT
//            _ <- resourceUserRepository.save(
//              ResourceUser(
//                resourceId = resource.id,
//                userId = user.id,
//                resourceUserType = ResourceUserType.BOUGHT,
//                created = clock.now
//              )
//            )
//
//            // Devolver confirmación
//            json <- Future.successful(
//              Json.obj(
//                "resourceId" -> resource.id.toString,
//                "userId" -> user.id.toString,
//                "accessType" -> ResourceUserType.BOUGHT.toString,
//                "grantedAt" -> clock.now.toString
//              )
//            )
//          } yield {
//            Created(json)
//          }
//      }
//    }
//
//  /**
//   * Ofrece un recurso como forma de pago (intercambio/trueque)
//   * POST /api/resources/:resourceId/offer-as-payment/:toUserId
//   */
//  def offerResourceAsPayment(resourceId: Long, toUserId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Validar que es propietario
//            _ <- isResourceOwnerOrThrow(resource.id, user.id)
//
//            // Otorgar acceso como ACCEPTED_AS_PAYMENT
//            _ <- resourceUserRepository.save(
//              ResourceUser(
//                resourceId = resource.id,
//                userId = UserId(toUserId),
//                resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
//                created = clock.now
//              )
//            )
//
//            // Devolver confirmación
//            json <- Future.successful(
//              Json.obj(
//                "resourceId" -> resource.id.toString,
//                "toUserId" -> UserId(toUserId).toString,
//                "accessType" -> ResourceUserType.ACCEPTED_AS_PAYMENT.toString,
//                "grantedAt" -> clock.now.toString
//              )
//            )
//          } yield {
//            Created(json)
//          }
//      }
//    }
//
//  /**
//   * Otorga acceso genérico a un recurso (solo propietario)
//   * POST /api/resources/:resourceId/grant-access
//   * Body: { "userId": 123, "accessType": "BOUGHT" | "ACCEPTED_AS_PAYMENT" }
//   */
//  def grantAccess(resourceId: Long): Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))
//
//          val targetUserId = (body \ "userId").asOpt[Long]
//            .getOrElse(throw BadRequestException("Missing userId"))
//
//          val accessType = (body \ "accessType").asOpt[String]
//            .flatMap(at => scala.util.Try(ResourceUserType.withNameInsensitive(at)).toOption)
//            .getOrElse(throw BadRequestException("Invalid accessType"))
//
//          // No permitir cambiar a OWNER
//          if (accessType == ResourceUserType.OWNER) {
//            throw BadRequestException("Cannot grant OWNER access through this endpoint")
//          }
//
//          for {
//            // Obtener el Resource
//            resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
//            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))
//
//            // Validar que es propietario
//            _ <- isResourceOwnerOrThrow(resource.id, user.id)
//
//            // Otorgar el acceso
//            _ <- resourceUserRepository.save(
//              ResourceUser(
//                resourceId = resource.id,
//                userId = UserId(targetUserId),
//                resourceUserType = accessType,
//                created = clock.now
//              )
//            )
//
//            // Devolver confirmación
//            json <- Future.successful(
//              Json.obj(
//                "resourceId" -> resource.id.toString,
//                "userId" -> UserId(targetUserId).toString,
//                "accessType" -> accessType.toString,
//                "grantedAt" -> clock.now.toString
//              )
//            )
//          } yield {
//            Created(json)
//          }
//      }
//    }
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
