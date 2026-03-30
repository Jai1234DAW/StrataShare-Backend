package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.sample.SampleRepository
import dev.pompilius.resource.domain.study.StudyRepository
import dev.pompilius.resource.domain.{ResourceId, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType, SampleResource}
import dev.pompilius.resource.infrastructure.parsers.CreateSampleRequestParser
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.BaseController
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import dev.pompilius.users.domain.{Role, UserId}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller para operaciones específicas de SampleResource
 */
@Singleton
class SampleController @Inject() (
    sampleRepository: SampleRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  /**
   * Crea un nuevo Sample
   * POST /api/resources/samples
   */
  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createSampleRequest = CreateSampleRequestParser.parse(request)

          val newSample = SampleResource(
            id = ResourceId.gen(configuration.nodeId),
            resourceType = ResourceType.SAMPLE,
            visibility = createSampleRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createSampleRequest.localization,
            observations = createSampleRequest.observations,
            summary = createSampleRequest.summary
            // Nota: Los campos específicos de Sample irían aquí si los tuviera
            // Por ahora SampleResource solo tiene los campos de Resource
          )

          for {
            _ <- sampleRepository.save(newSample)

            // Guardar la relación de propiedad
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = newSample.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            json <- resourceWriter.asOwner(newSample, None, None)
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Lista todos los Samples del usuario
   * GET /api/resources/samples
   */
  def list: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            userSamples <- sampleRepository.findAllByUser(user.id)

            sampleJsons <- Future.sequence(
              userSamples.map { sample =>
                resourceWriter.asOwner(sample, None, None)
              }
            )
          } yield {
            Ok(Json.obj(
              "data" -> sampleJsons,
              "total" -> sampleJsons.length
            ))
          }
      }
    }

  /**
   * Obtiene un Sample específico
   * GET /api/resources/samples/:sampleId
   */
  def get(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            sampleOpt <- sampleRepository.findById(ResourceId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            _ <- Future.successful(
              if (sample.deleted) throw NotFoundException("Sample not found")
            )

            _ <- validateResourceAccess(sample, user.id)

            isOwner <- isResourceOwner(sample.id, user.id)

            json <- if (isOwner) {
              resourceWriter.asOwner(sample, None, None)
            } else {
              resourceWriter.asPublic(sample, None, None)
            }
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Actualiza un Sample
   * PUT /api/resources/samples/:sampleId
   */
  def update(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

          for {
            sampleOpt <- sampleRepository.findById(ResourceId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            _ <- isResourceOwnerOrThrow(sample.id, user.id)

            observations = (body \ "observations").asOpt[String].orElse(sample.observations)
            summary = (body \ "summary").asOpt[String].orElse(sample.summary)
            visibility = (body \ "visibility").asOpt[String]
              .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)

            updatedSample = sample.copy(
              observations = observations,
              summary = summary,
              visibility = visibility.getOrElse(sample.visibility),
              updated = clock.now
            )

            _ <- sampleRepository.save(updatedSample)

            json <- resourceWriter.asOwner(updatedSample, None, None)
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Borra un Sample
   * DELETE /api/resources/samples/:sampleId
   */
  def delete(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            sampleOpt <- sampleRepository.findById(ResourceId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            _ <- isResourceOwnerOrThrow(sample.id, user.id)

            deletedSample = sample.copy(deleted = true, updated = clock.now)
            _ <- sampleRepository.save(deletedSample)
          } yield {
            NoContent
          }
      }
    }

  // ===== MÉTODOS PRIVADOS =====

  private def validateResourceAccess(resource: SampleResource, userId: UserId): Future[Unit] = {
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

