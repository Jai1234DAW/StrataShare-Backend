package dev.pompilius.study.infrastructure.controllers

import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.study.infrastructure.parsers.{CreateStudyRequestParser, UpdateStudyRequestParser}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.study.domain.{Study, StudyId, StudyRepository}
import dev.pompilius.shared.domain.Pagination
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter
)(implicit val ec: ExecutionContext)
  extends BaseController {


  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createStudyRequest = CreateStudyRequestParser.parse(request)

          val resourceId = ResourceId.gen(configuration.nodeId)
          val studyId = StudyId.gen(configuration.nodeId)

          // 1. Crear el Resource (datos comunes)
          val newResource = Resource(
           id = resourceId,
            resourceType = ResourceType.STUDY,
            visibility = createStudyRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createStudyRequest.localization,
            observations = createStudyRequest.observations,
            summary = createStudyRequest.summary
          )

          // 2. Crear el Study (datos específicos)
          val newStudy = Study(
            id = studyId,
            resourceId = resourceId,
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

          for {
            // 1. Guardar Resource
            _ <- resourceRepository.save(newResource)

            // 2. Guardar Study
            _ <- studyRepository.save(newStudy)

            // 3. Vincular a usuario
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = resourceId,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            // 4. Retornar JSON
            json <- resourceWriter.asOwner(newResource, None, Some(newStudy))
          } yield {
            Ok(json)
          }
      }
    }

  def update(studyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val updateStudyRequest = UpdateStudyRequestParser.parse(request)

          for {
            // Obtener el Study
            study <- studyRepository.findById(StudyId(studyId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Study with id $studyId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(study.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study $studyId"))
            )

            // Obtener ResourceUser y validar permisos
            resourceUser <- resourceUserRepository.findByResourceAndUser(resource.id, user.id).map(
              _.getOrElse(throw new Exception("You don't have permission to update this resource"))
            )

            _ <- Future.successful(
              if (resourceUser.resourceUserType != ResourceUserType.OWNER)
                throw new Exception("Only the resource owner can update this resource")
            )

            updatedResource = resource.copy(
              visibility = updateStudyRequest.visibility.getOrElse(resource.visibility),
              localization = updateStudyRequest.localization.getOrElse(resource.localization),
              observations = updateStudyRequest.observations.orElse(resource.observations),
              summary = updateStudyRequest.summary.orElse(resource.summary),
              updated = clock.now
            )

            // Actualizar el Study (datos específicos)
            updatedStudy = study.copy(
              name = updateStudyRequest.name.getOrElse(study.name),
              startDate = updateStudyRequest.startDate.getOrElse(study.startDate),
              endDate = updateStudyRequest.endDate.orElse(study.endDate),
              description = updateStudyRequest.description.getOrElse(study.description),
              coordinates = updateStudyRequest.coordinates.getOrElse(study.coordinates),
              area = updateStudyRequest.area.getOrElse(study.area),
              methods = updateStudyRequest.methods.getOrElse(study.methods),
              authors = updateStudyRequest.authors.getOrElse(study.authors),
              section = updateStudyRequest.section.getOrElse(study.section),
              antecedents = updateStudyRequest.antecedents.getOrElse(study.antecedents),
              nameSection = updateStudyRequest.nameSection.orElse(study.nameSection)
            )

            // ...existing code...
            _ <- resourceRepository.save(updatedResource)
            _ <- studyRepository.save(updatedStudy)

            // Retornar JSON actualizado
            json <- resourceWriter.asOwner(updatedResource, None, Some(updatedStudy))
          } yield {
            Ok(json)
          }
      }
    }

  def get(studyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            // Obtener el Study
            study <- studyRepository.findById(StudyId(studyId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Study with id $studyId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(study.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study $studyId"))
            )

            // Validar acceso según visibilidad
            _ <- validateAccess(resource, user.id)

            // Retornar JSON del estudio completo
            json <- resourceWriter.asPublic(resource, None, Some(study))
          } yield {
            Ok(json)
          }
      }
    }

  def delete(studyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Study
            study <- studyRepository.findById(StudyId(studyId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Study with id $studyId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(study.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study $studyId"))
            )

            // Obtener ResourceUser con todas sus validaciones
            resourceUser <- resourceUserRepository.findByResourceAndUser(resource.id, user.id).map(
              _.getOrElse(throw new Exception("You don't have permission to delete this resource"))
            )

            // Validar que sea propietario y no esté eliminado
            _ <- Future.successful(
              if (resourceUser.resourceUserType != ResourceUserType.OWNER)
                throw new Exception("Only the resource owner can delete this resource")
            )

            _ <- Future.successful(
              if (resourceUser.deleted)
                throw new Exception("This resource has already been deleted")
            )

            // Marcar ResourceUser como eliminado (soft delete)
            _ <- resourceUserRepository.save(
              resourceUser.copy(deleted = true)
            )

            // Retornar confirmación
          } yield {
            Ok(Json.obj("message" -> "Study deleted successfully"))
          }
      }
    }

  private def validateAccess(resource: Resource, userId: UserId): Future[Unit] = {
    import dev.pompilius.shared.domain.Visibility

    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(())

      case Visibility.PRIVATE =>
        // Privado: solo el propietario puede acceder
        resourceUserRepository.findByResourceAndUser(resource.id, userId).map {
          case Some(ru) if ru.resourceUserType == ResourceUserType.OWNER && !ru.deleted => ()
          case _ => throw new Exception("You don't have permission to access this private resource")
        }
    }
  }
}