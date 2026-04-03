package dev.pompilius.study.infrastructure.controllers

import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{
  Resource,
  ResourceId,
  ResourceRepository,
  ResourceType,
  ResourceUser,
  ResourceUserRepository,
  ResourceUserType
}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.study.infrastructure.parsers.{CreateStudyRequestParser, UpdateStudyRequestParser}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.study.domain.{Area, Study, StudyFilter, StudyId, StudyRepository}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json
import org.joda.time.DateTime

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter,
    resourceAccessValidator: ResourceAccessValidator,
    paginatedWriter: PaginatedWriter
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

            _ <- studyRepository.save(newStudy).recoverWith {
              case NonFatal(e) =>
                // Si falla guardar el Study, eliminar el Resource para no dejar datos huérfanos
                resourceRepository.delete(resourceId).map(_ => throw e)
            }

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

            json <- resource.visibility match {
              // Si es PÚBLICO → Todos ven datos completos
              case Visibility.PUBLIC =>
                resourceWriter.asPublic(resource, None, Some(study))

              // Si es PRIVADO → Verificar tipo de acceso del usuario
              case Visibility.PRIVATE =>
                resourceUserRepository.findByResourceAndUser(resource.id, user.id).flatMap {
                  case Some(ru) if !ru.deleted && ru.resourceUserType == ResourceUserType.OWNER =>
                    // Es el propietario → Vista completa con permisos
                    resourceWriter.asOwner(resource, None, Some(study))

                  case Some(ru)
                      if !ru.deleted &&
                        (ru.resourceUserType == ResourceUserType.PURCHASED ||
                          ru.resourceUserType == ResourceUserType.ACCEPTED_AS_PAYMENT) =>
                    // Lo compró o recibió como pago → Vista completa sin permisos
                    resourceWriter.asPublic(resource, None, Some(study))

                  case _ =>
                    // NO tiene acceso → Solo preview/teaser
                    resourceWriter.asPrivate(resource, None, Some(study))
                }
            }

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

          } yield {
            Ok
          }
      }
    }

  def getAll(
      name: Option[String],
      year: Option[Int],
      area: Option[String],
      authors: Option[String],
      search: Option[String],
      userId: Option[String],
      visibility: Option[String],
      localization: Option[String],
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
                year = year,
                area = area.map(Area.withNameInsensitive),
                authors = authors,
                search = search,
                visibility= visibility.map(Visibility.withNameInsensitive),
                localization = localization,
                userId = userId.map(UserId(_)) // ← Opcional: si es None, busca en todos
              ),
              pag.oneMore
            )
            // 2. Para cada study, obtener resource y generar preview JSON usando paginatedWriter
            json <- paginatedWriter.toJson(Paginated(studies, pag)) { study =>
              for {
                resource <-
                  resourceRepository
                    .findById(study.resourceId)
                    .map(
                      _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study ${study.id}"))
                    )
                // Siempre devolver preview (datos básicos para el listado)
                //Luego se pordrá acceder con más datos a cada uno de ellos.
                json <- resourceWriter.asPrivate(resource, None, Some(study))
              } yield json
            }

          } yield {
            Ok(json)
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
