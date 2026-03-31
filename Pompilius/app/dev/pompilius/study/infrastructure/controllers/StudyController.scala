package dev.pompilius.study.infrastructure.controllers

import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.study.infrastructure.parsers.{CreateStudyRequestParser, UpdateStudyRequestParser}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.study.domain.{Area, Study, StudyFilter, StudyId, StudyRepository}
import dev.pompilius.resource.ResourceAccessValidator
import dev.pompilius.shared.domain.{Pagination, Visibility}
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json
import org.joda.time.DateTime

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter,
    resourceAccessValidator: ResourceAccessValidator,
    paginatedWriter:PaginatedWriter

)(implicit val ec: ExecutionContext)
    extends BaseController {

  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createStudyRequest = CreateStudyRequestParser.parse(request)

          val resourceId = ResourceId.gen(configuration.nodeId)
          val studyId = StudyId.gen(configuration.nodeId)

          // Crear el Resource (datos comunes)
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

          // Crear el Study (datos específicos)
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

            _ <- resourceRepository.save(newResource)

            _ <- studyRepository.save(newStudy)

            // Vincular a usuario
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = resourceId,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

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
            //método simplificado está más abajo para reutilizarlo en get y delete
            (study, resource) <- getStudyWithResource(studyId)

            // Verificar que es propietario del resource y que no está borrado lógicamente
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

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
            (study, resource) <- getStudyWithResource(studyId)

            // Validar acceso según visibilidad
            _ <- resourceAccessValidator.validateAccess(resource, user.id)

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
            (_, resource) <- getStudyWithResource(studyId)

            // Verificar que es propietario
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

            // Marcar ResourceUser como eliminado (soft delete)
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = resource.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now,
                deleted = true
              )
            )

            // Retornar confirmación
          } yield {
            Ok
          }
      }
    }

  def getAll(
      name: Option[String],
      area: Option[String],
      startDate: Option[DateTime],
      endDate: Option[DateTime],
      authors: Option[String],
      search: Option[String],
      userId: Option[String],
      pag: Pagination
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, currentUser, _) =>

          for {
            // 1. Buscar studies con todos los filtros
            studies <- studyRepository.find(
              StudyFilter(
                name = name,
                startDate = startDate,
                endDate = endDate,
                area = area.map(Area.withNameInsensitive),
                authors = authors,
                search = search,
                userId = userId.map(UserId(_))  // ← Opcional: si es None, busca en todos
              ),
              pag=pag.oneMore
            )

            // 2. Para cada study, obtener su resource asociado
            studiesWithResources <- Future.sequence(studies.map { study =>
              resourceRepository.findById(study.resourceId)
              }
            })

           jsonList<-resourceWriter.asPublic(studiesWithResources)
          } yield {
            Ok(Json.toJson(jsonList))
          }
      }
    }

  private def getStudyWithResource(studyId: String): Future[(Study, Resource)] = {
    for {
      study <-
        studyRepository
          .findById(StudyId(studyId))
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Sample with id $studyId not found"))
          )

      resource <-
        resourceRepository
          .findById(study.resourceId)
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $studyId"))
          )
    } yield (study, resource)
  }

}
