package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.sample.SampleRepository
import dev.pompilius.resource.domain.study.StudyRepository
import dev.pompilius.resource.domain.{ResourceId, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType, StudyResource}
import dev.pompilius.resource.infrastructure.parsers.CreateStudyRequestParser
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
 * Controller para operaciones específicas de StudyResource
 */
@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  /**
   * Crea un nuevo Study
   * POST /api/resources/studies
   */
  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createStudyRequest = CreateStudyRequestParser.parse(request)

          val newStudy = StudyResource(
            id = ResourceId.gen(configuration.nodeId),
            resourceType = ResourceType.STUDY,
            visibility = createStudyRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createStudyRequest.localization,
            observations = createStudyRequest.observations,
            summary = createStudyRequest.summary
            // Nota: Los campos específicos de Study irían aquí si los tuviera
            // Por ahora StudyResource solo tiene los campos de Resource
          )

          for {
            _ <- studyRepository.save(newStudy)

            // Guardar la relación de propiedad
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = newStudy.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            json <- resourceWriter.asOwner(newStudy, None, None)
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Lista todos los Studies del usuario
   * GET /api/resources/studies
   */
  def list: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            userStudies <- studyRepository.findAllByUser(user.id)

            studyJsons <- Future.sequence(
              userStudies.map { study =>
                resourceWriter.asOwner(study, None, None)
              }
            )
          } yield {
            Ok(Json.obj(
              "data" -> studyJsons,
              "total" -> studyJsons.length
            ))
          }
      }
    }

  /**
   * Obtiene un Study específico
   * GET /api/resources/studies/:studyId
   */
  def get(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            studyOpt <- studyRepository.findById(ResourceId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            _ <- Future.successful(
              if (study.deleted) throw NotFoundException("Study not found")
            )

            _ <- validateResourceAccess(study, user.id)

            isOwner <- isResourceOwner(study.id, user.id)

            json <- if (isOwner) {
              resourceWriter.asOwner(study, None, None)
            } else {
              resourceWriter.asPublic(study, None, None)
            }
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Actualiza un Study
   * PUT /api/resources/studies/:studyId
   */
  def update(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

          for {
            studyOpt <- studyRepository.findById(ResourceId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            _ <- isResourceOwnerOrThrow(study.id, user.id)

            observations = (body \ "observations").asOpt[String].orElse(study.observations)
            summary = (body \ "summary").asOpt[String].orElse(study.summary)
            visibility = (body \ "visibility").asOpt[String]
              .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)

            updatedStudy = study.copy(
              observations = observations,
              summary = summary,
              visibility = visibility.getOrElse(study.visibility),
              updated = clock.now
            )

            _ <- studyRepository.save(updatedStudy)

            json <- resourceWriter.asOwner(updatedStudy, None, None)
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Borra un Study
   * DELETE /api/resources/studies/:studyId
   */
  def delete(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            studyOpt <- studyRepository.findById(ResourceId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            _ <- isResourceOwnerOrThrow(study.id, user.id)

            deletedStudy = study.copy(deleted = true, updated = clock.now)
            _ <- studyRepository.save(deletedStudy)
          } yield {
            NoContent
          }
      }
    }

  // ===== MÉTODOS PRIVADOS =====

  private def validateResourceAccess(resource: StudyResource, userId: UserId): Future[Unit] = {
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

