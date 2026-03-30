package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.sample.SampleRepository
import dev.pompilius.resource.domain.study.StudyRepository
import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType, SampleResource, StudyResource}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import dev.pompilius.users.domain.{Role, UserId}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller para operaciones generales de Resource
 * Maneja operaciones que funcionan en cualquier tipo de recurso (SampleResource, StudyResource)
 */
@Singleton
class ResourceController @Inject() (
    sampleRepository: SampleRepository,
    studyRepository: StudyRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter,
    paginatedWriter: PaginatedWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  /**
   * Lista todos los recursos del usuario (Samples + Studies)
   * GET /api/resources?page=1&pageSize=10
   */
  def list(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>

          val userSamplesF: Future[Seq[SampleResource]] = sampleRepository.findAllByUser(user.id)
          val userStudiesF: Future[Seq[StudyResource]] = studyRepository.findAllByUser(user.id)

          val sampleJsonsF: Future[Seq[JsValue]] = userSamplesF.flatMap { userSamples =>
            Future.sequence(
              userSamples.map { sample =>
                if (!sample.deleted) {
                  resourceWriter.asOwner(sample, None, None)
                } else {
                  Future.successful(Json.obj())
                }
              }
            ).map(_.filter(_ != Json.obj()))
          }

          val studyJsonsF: Future[Seq[JsValue]] = userStudiesF.flatMap { userStudies =>
            Future.sequence(
              userStudies.map { study =>
                if (!study.deleted) {
                  resourceWriter.asOwner(study, None, None)
                } else {
                  Future.successful(Json.obj())
                }
              }
            ).map(_.filter(_ != Json.obj()))
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

  /**
   * Obtiene un recurso específico por ID (auto-detecta si es Sample o Study)
   * GET /api/resources/:resourceId
   */
  def get(resourceId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Intentar obtener como Sample
            sampleOpt <- sampleRepository.findById(ResourceId(resourceId))

            resource <- sampleOpt match {
              case Some(sample) =>
                // Es un Sample
                _ <- validateResourceAccess(sample, user.id)
                isOwner <- isResourceOwner(sample.id, user.id)
                json <- if (isOwner) {
                  resourceWriter.asOwner(sample, None, None)
                } else {
                  resourceWriter.asPublic(sample, None, None)
                }
                Future.successful(Json.obj("resource" -> json))

              case None =>
                // No es Sample, intentar Study
                for {
                  studyOpt <- studyRepository.findById(ResourceId(resourceId))
                  study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Resource not found")))

                  _ <- Future.successful(
                    if (study.deleted) throw NotFoundException("Resource not found")
                  )

                  _ <- validateResourceAccess(study, user.id)
                  isOwner <- isResourceOwner(study.id, user.id)
                  json <- if (isOwner) {
                    resourceWriter.asOwner(study, None, None)
                  } else {
                    resourceWriter.asPublic(study, None, None)
                  }
                } yield Json.obj("resource" -> json)
            }
          } yield {
            Ok(resource)
          }
      }
    }

  /**
   * Obtiene los recursos del usuario (sin paginación)
   * GET /api/resources/my
   */
  def getMyResources: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            userSamples <- sampleRepository.findAllByUser(user.id)
            userStudies <- studyRepository.findAllByUser(user.id)

            sampleJsons <- Future.sequence(
              userSamples.filter(!_.deleted).map { sample =>
                resourceWriter.asOwner(sample, None, None)
              }
            )

            studyJsons <- Future.sequence(
              userStudies.filter(!_.deleted).map { study =>
                resourceWriter.asOwner(study, None, None)
              }
            )

            allResources = sampleJsons ++ studyJsons
          } yield {
            Ok(Json.obj(
              "data" -> allResources,
              "total" -> allResources.length
            ))
          }
      }
    }

  /**
   * Borra un recurso (soft delete)
   * DELETE /api/resources/:resourceId
   */
  def delete(resourceId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Intentar obtener como Sample
            sampleOpt <- sampleRepository.findById(ResourceId(resourceId))

            _ <- sampleOpt match {
              case Some(sample) =>
                for {
                  _ <- isResourceOwnerOrThrow(sample.id, user.id)
                  deletedSample = sample.copy(deleted = true, updated = clock.now)
                  _ <- sampleRepository.save(deletedSample)
                } yield ()

              case None =>
                // Intentar como Study
                for {
                  studyOpt <- studyRepository.findById(ResourceId(resourceId))
                  study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Resource not found")))
                  _ <- isResourceOwnerOrThrow(study.id, user.id)
                  deletedStudy = study.copy(deleted = true, updated = clock.now)
                  _ <- studyRepository.save(deletedStudy)
                } yield ()
            }
          } yield {
            NoContent
          }
      }
    }

  /**
   * Obtiene los usuarios con acceso a un recurso
   * GET /api/resources/:resourceId/users
   */
  def getResourceUsers(resourceId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            _ <- isResourceOwnerOrThrow(ResourceId(resourceId), user.id)

            resourceUsers <- resourceUserRepository.getAllByResourceId(ResourceId(resourceId))

            json <- Future.successful(
              Json.obj(
                "resourceId" -> resourceId.toString,
                "users" -> resourceUsers.map(ru =>
                  Json.obj(
                    "userId" -> ru.userId.toString,
                    "accessType" -> ru.resourceUserType.toString,
                    "grantedAt" -> ru.created.toString
                  )
                )
              )
            )
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Compra un recurso
   * POST /api/resources/:resourceId/purchase
   */
  def purchase(resourceId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            currentAccessOpt <- resourceUserRepository.findByResourceAndUser(ResourceId(resourceId), user.id)

            _ <- Future.successful(
              currentAccessOpt match {
                case Some(access) =>
                  access.resourceUserType match {
                    case ResourceUserType.OWNER =>
                      throw BadRequestException("Cannot purchase your own resource")
                    case ResourceUserType.BOUGHT =>
                      throw BadRequestException("You already own access to this resource")
                    case ResourceUserType.ACCEPTED_AS_PAYMENT =>
                      throw BadRequestException("You already have access to this resource through payment")
                    case _ =>
                      throw BadRequestException("You already have access to this resource")
                  }
                case None => ()
              }
            )

            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = ResourceId(resourceId),
                userId = user.id,
                resourceUserType = ResourceUserType.BOUGHT,
                created = clock.now
              )
            )

            json <- Future.successful(
              Json.obj(
                "resourceId" -> resourceId.toString,
                "userId" -> user.id.toString,
                "accessType" -> ResourceUserType.BOUGHT.toString,
                "grantedAt" -> clock.now.toString
              )
            )
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Ofrece un recurso como pago
   * POST /api/resources/:resourceId/offer-as-payment/:toUserId
   */
  def offerResourceAsPayment(resourceId: Long, toUserId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            _ <- isResourceOwnerOrThrow(ResourceId(resourceId), user.id)

            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = ResourceId(resourceId),
                userId = UserId(toUserId),
                resourceUserType = ResourceUserType.ACCEPTED_AS_PAYMENT,
                created = clock.now
              )
            )

            json <- Future.successful(
              Json.obj(
                "resourceId" -> resourceId.toString,
                "toUserId" -> toUserId.toString,
                "accessType" -> ResourceUserType.ACCEPTED_AS_PAYMENT.toString,
                "grantedAt" -> clock.now.toString
              )
            )
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Otorga acceso a un recurso
   * POST /api/resources/:resourceId/grant-access
   */
  def grantAccess(resourceId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

          val targetUserId = (body \ "userId").asOpt[Long]
            .getOrElse(throw BadRequestException("Missing userId"))

          val accessType = (body \ "accessType").asOpt[String]
            .flatMap(at => scala.util.Try(ResourceUserType.withNameInsensitive(at)).toOption)
            .getOrElse(throw BadRequestException("Invalid accessType"))

          if (accessType == ResourceUserType.OWNER) {
            throw BadRequestException("Cannot grant OWNER access through this endpoint")
          }

          for {
            _ <- isResourceOwnerOrThrow(ResourceId(resourceId), user.id)

            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = ResourceId(resourceId),
                userId = UserId(targetUserId),
                resourceUserType = accessType,
                created = clock.now
              )
            )

            json <- Future.successful(
              Json.obj(
                "resourceId" -> resourceId.toString,
                "userId" -> targetUserId.toString,
                "accessType" -> accessType.toString,
                "grantedAt" -> clock.now.toString
              )
            )
          } yield {
            Created(json)
          }
      }
    }

  // ===== MÉTODOS PRIVADOS =====

  private def validateResourceAccess(resource: Resource, userId: UserId): Future[Unit] = {
    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(())

      case Visibility.PRIVATE =>
        isResourceOwner(resource.id, userId).map {
          case true => ()
          case false => throw ForbiddenException("This resource is private")
        }

      case _ =>
        resourceUserRepository.findByResourceAndUser(resource.id, userId).map {
          case Some(_) => ()
          case None => throw ForbiddenException("You don't have access to this resource")
        }
    }
  }

  private def isResourceOwner(resourceId: ResourceId, userId: UserId): Future[Boolean] = {
    resourceUserRepository.findByResourceAndUser(resourceId, userId).map {
      case Some(ru) => ru.resourceUserType == ResourceUserType.OWNER
      case None => false
    }
  }

  private def isResourceOwnerOrThrow(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    isResourceOwner(resourceId, userId).map {
      case true => ()
      case false => throw ForbiddenException("Only the resource owner can perform this action")
    }
  }
}

