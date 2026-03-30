package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.request.CreateStudyRequest
import dev.pompilius.resource.domain.sample.SampleRepository
import dev.pompilius.resource.domain.study.{Study, StudyId, StudyRepository}
import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.resource.infrastructure.parsers.CreateStudyRequestParser
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

/**
 * Controller para operaciones específicas de Study
 * Maneja: crear, listar, obtener, actualizar y eliminar Studies
 */
@Singleton
class StudyController @Inject() (
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    sampleRepository: SampleMySqlRepository,
    studyRepository: StudyMySqlRepository,
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

          // 1. Crear el Resource base
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

          // 2. Crear el Study específico
          val newStudy = Study(
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

          // 3. Guardar en BD
          for {
            _ <- resourceRepository.save(newResource)
            _ <- studyRepository.save(newStudy)

            // Guardar la relación de propiedad
            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = newResource.id,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )

            // Devolver el JSON
            json <- resourceWriter.asOwner(newResource, None, Some(newStudy))
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Lista todos los Studies públicos y propios del usuario
   * GET /api/resources/studies?localization=...&page=...&pageSize=...
   */
  def list: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          // Parámetros de query
          val localization = request.queryString.get("localization").flatMap(_.headOption)
          val page = request.queryString.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(1)
          val pageSize = request.queryString.get("pageSize").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(10)

          // Filtro básico por tipo de recurso
          val filter = ResourceFilter(
            resourceType = Some(ResourceType.STUDY),
            visibility = None,
            localization = localization
          )

          for {
            // Obtener recursos del usuario
            userResources <- resourceUserRepository.getAllByUserId(user.id)
            userResourceIds = userResources.map(_.resourceId).toSet

            // Obtener recursos públicos con el filtro
            publicResources <- resourceRepository.find(filter, Pagination(page, pageSize))
            publicResourceIds = publicResources.data.filter(_.visibility == Visibility.PUBLIC).map(_.id).toSet

            // Combinar ambos conjuntos (sin duplicados)
            allResourceIds = userResourceIds ++ publicResourceIds

            // Obtener los datos completos
            resources <- Future.sequence(
              allResourceIds.map { resId =>
                for {
                  resourceOpt <- resourceRepository.findById(resId)
                  studyOpt <- resourceOpt match {
                    case Some(res) if res.resourceType == ResourceType.STUDY =>
                      studyRepository.findByResourceId(resId)
                    case _ => Future.successful(None)
                  }
                } yield (resourceOpt, studyOpt)
              }
            ).map(_.filter { case (_, studyOpt) => studyOpt.isDefined }.map { case (r, s) => (r, s) })

            // Convertir a JSON
            jsonResources <- Future.sequence(
              resources.map { case (resource, study) =>
                resourceWriter.asPublic(resource.get, None, study)
              }
            )
          } yield {
            Ok(Json.obj(
              "data" -> jsonResources,
              "page" -> page,
              "pageSize" -> pageSize,
              "total" -> resources.length
            ))
          }
      }
    }

  /**
   * Obtiene un Study específico por ID
   * GET /api/resources/studies/:studyId
   */
  def get(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Study
            studyOpt <- studyRepository.findById(StudyId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            // Obtener el Resource asociado
            resourceOpt <- resourceRepository.findById(study.resourceId)
            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))

            // Validar que no está borrado
            _ <- Future.successful(
              if (resource.deleted) throw NotFoundException("Study not found")
            )

            // Validar acceso
            _ <- validateResourceAccess(resource, user.id)

            // Determinar si es propietario
            isOwner <- isResourceOwner(resource.id, user.id)

            // Generar JSON
            json <- if (isOwner) {
              resourceWriter.asOwner(resource, None, Some(study))
            } else {
              resourceWriter.asPublic(resource, None, Some(study))
            }
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Actualiza un Study existente (solo propietario)
   * PUT /api/resources/studies/:studyId
   */
  def update(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

          for {
            // Obtener el Study
            studyOpt <- studyRepository.findById(StudyId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            // Obtener el Resource
            resourceOpt <- resourceRepository.findById(study.resourceId)
            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))

            // Validar que es propietario
            _ <- isResourceOwnerOrThrow(resource.id, user.id)

            // Extraer datos del body
            name = (body \ "name").asOpt[String].getOrElse(study.name)
            startDate = (body \ "startDate").asOpt[String]
              .map(dateStr => new org.joda.time.DateTime(dateStr))
              .getOrElse(study.startDate)
            endDate = (body \ "endDate").asOpt[String]
              .map(dateStr => new org.joda.time.DateTime(dateStr))
              .orElse(study.endDate)
            description = (body \ "description").asOpt[String].getOrElse(study.description)
            coordinates = (body \ "coordinates").asOpt[String].getOrElse(study.coordinates)
            area = (body \ "area").asOpt[String]
              .flatMap(a => scala.util.Try(dev.pompilius.resource.domain.study.Area.withNameInsensitive(a)).toOption)
              .getOrElse(study.area)
            methods = (body \ "methods").asOpt[String].getOrElse(study.methods)
            authors = (body \ "authors").asOpt[String].getOrElse(study.authors)
            section = (body \ "section").asOpt[Boolean].getOrElse(study.section)
            antecedents = (body \ "antecedents").asOpt[Boolean].getOrElse(study.antecedents)
            nameSection = (body \ "nameSection").asOpt[String].orElse(study.nameSection)

            // Actualizar metadatos del Resource
            observations = (body \ "observations").asOpt[String].orElse(resource.observations)
            summary = (body \ "summary").asOpt[String].orElse(resource.summary)
            visibility = (body \ "visibility").asOpt[String]
              .flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)

            updatedResource = resource.copy(
              observations = observations,
              summary = summary,
              visibility = visibility.getOrElse(resource.visibility),
              updated = clock.now
            )

            updatedStudy = study.copy(
              name = name,
              startDate = startDate,
              endDate = endDate,
              description = description,
              coordinates = coordinates,
              area = area,
              methods = methods,
              authors = authors,
              section = section,
              antecedents = antecedents,
              nameSection = nameSection
            )

            // Guardar cambios
            _ <- resourceRepository.save(updatedResource)
            _ <- studyRepository.save(updatedStudy)

            // Devolver el JSON actualizado
            json <- resourceWriter.asOwner(updatedResource, None, Some(updatedStudy))
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Borra un Study (soft delete)
   * DELETE /api/resources/studies/:studyId
   */
  def delete(studyId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Study
            studyOpt <- studyRepository.findById(StudyId(studyId))
            study <- Future.successful(studyOpt.getOrElse(throw NotFoundException("Study not found")))

            // Obtener el Resource
            resourceOpt <- resourceRepository.findById(study.resourceId)
            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))

            // Validar que es propietario
            _ <- isResourceOwnerOrThrow(resource.id, user.id)

            // Soft delete
            deletedResource = resource.copy(deleted = true, updated = clock.now)
            _ <- resourceRepository.save(deletedResource)
          } yield {
            NoContent
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

