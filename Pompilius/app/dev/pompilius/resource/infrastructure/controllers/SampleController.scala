package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.resource.domain.request.CreateSampleRequest
import dev.pompilius.resource.domain.sample.{Sample, SampleId, SampleRepository}
import dev.pompilius.resource.domain.study.StudyRepository
import dev.pompilius.resource.domain.{Resource, ResourceFilter, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.resource.infrastructure.parsers.CreateSampleRequestParser
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
 * Controller para operaciones específicas de Sample
 * Maneja: crear, listar, obtener, actualizar y eliminar Samples
 */
@Singleton
class SampleController @Inject() (
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    sampleRepository: SampleMySqlRepository,
    studyRepository: StudyMySqlRepository,
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

          // 1. Crear el Resource base
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

          // 2. Crear el Sample específico
          val newSample = Sample(
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

          // 3. Guardar en BD
          for {
            _ <- resourceRepository.save(newResource)
            _ <- sampleRepository.save(newSample)

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
            json <- resourceWriter.asOwner(newResource, Some(newSample), None)
          } yield {
            Created(json)
          }
      }
    }

  /**
   * Lista todos los Samples públicos y propios del usuario
   * GET /api/resources/samples?localization=...&minerals=...&isFresh=...&page=...&pageSize=...
   */
  def list: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          // Parámetros de query
          val localization = request.queryString.get("localization").flatMap(_.headOption)
          val minerals = request.queryString.get("minerals").flatMap(_.headOption)
          val isFresh = request.queryString.get("isFresh").flatMap(_.headOption).flatMap(_.toBooleanOption)
          val page = request.queryString.get("page").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(1)
          val pageSize = request.queryString.get("pageSize").flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(10)

          // Filtro básico por tipo de recurso
          val filter = ResourceFilter(
            resourceType = Some(ResourceType.SAMPLE),
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
                  sampleOpt <- resourceOpt match {
                    case Some(res) if res.resourceType == ResourceType.SAMPLE =>
                      sampleRepository.findByResourceId(resId)
                    case _ => Future.successful(None)
                  }
                } yield (resourceOpt, sampleOpt)
              }
            ).map(_.filter { case (_, sampleOpt) => sampleOpt.isDefined }.map { case (r, s) => (r, s) })

            // Filtrar por parámetros específicos si están presentes
            filtered = resources.filter { case (_, sampleOpt) =>
              sampleOpt match {
                case Some(sample) =>
                  val mineralMatch = minerals.forall(m => sample.minerals.exists(_.contains(m)))
                  val freshMatch = isFresh.forall(_ == sample.isFresh)
                  mineralMatch && freshMatch
                case None => false
              }
            }

            // Convertir a JSON
            jsonResources <- Future.sequence(
              filtered.map { case (resource, sample) =>
                resourceWriter.asPublic(resource.get, sample, None)
              }
            )
          } yield {
            Ok(Json.obj(
              "data" -> jsonResources,
              "page" -> page,
              "pageSize" -> pageSize,
              "total" -> filtered.length
            ))
          }
      }
    }

  /**
   * Obtiene un Sample específico por ID
   * GET /api/resources/samples/:sampleId
   */
  def get(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Sample
            sampleOpt <- sampleRepository.findById(SampleId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            // Obtener el Resource asociado
            resourceOpt <- resourceRepository.findById(sample.resourceId)
            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))

            // Validar que no está borrado
            _ <- Future.successful(
              if (resource.deleted) throw NotFoundException("Sample not found")
            )

            // Validar acceso
            _ <- validateResourceAccess(resource, user.id)

            // Determinar si es propietario
            isOwner <- isResourceOwner(resource.id, user.id)

            // Generar JSON
            json <- if (isOwner) {
              resourceWriter.asOwner(resource, Some(sample), None)
            } else {
              resourceWriter.asPublic(resource, Some(sample), None)
            }
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Actualiza un Sample existente (solo propietario)
   * PUT /api/resources/samples/:sampleId
   */
  def update(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

          for {
            // Obtener el Sample
            sampleOpt <- sampleRepository.findById(SampleId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            // Obtener el Resource
            resourceOpt <- resourceRepository.findById(sample.resourceId)
            resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException("Resource not found")))

            // Validar que es propietario
            _ <- isResourceOwnerOrThrow(resource.id, user.id)

            // Extraer datos del body
            name = (body \ "name").asOpt[String].getOrElse(sample.name)
            minerals = (body \ "minerals").asOpt[String].orElse(sample.minerals)
            collectionMethods = (body \ "collectionMethods").asOpt[String].orElse(sample.collectionMethods)
            isFresh = (body \ "isFresh").asOpt[Boolean].getOrElse(sample.isFresh)
            sampleType = (body \ "sampleType").asOpt[String].orElse(sample.sampleType)
            materialsUsed = (body \ "materialsUsed").asOpt[String].orElse(sample.materialsUsed)
            rockType = (body \ "rockType").asOpt[String].orElse(sample.rockType)
            geologicalProcesses = (body \ "geologicalProcesses").asOpt[String].orElse(sample.geologicalProcesses)

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

            updatedSample = sample.copy(
              name = name,
              minerals = minerals,
              collectionMethods = collectionMethods,
              isFresh = isFresh,
              sampleType = sampleType,
              materialsUsed = materialsUsed,
              rockType = rockType,
              geologicalProcesses = geologicalProcesses
            )

            // Guardar cambios
            _ <- resourceRepository.save(updatedResource)
            _ <- sampleRepository.save(updatedSample)

            // Devolver el JSON actualizado
            json <- resourceWriter.asOwner(updatedResource, Some(updatedSample), None)
          } yield {
            Ok(json)
          }
      }
    }

  /**
   * Borra un Sample (soft delete)
   * DELETE /api/resources/samples/:sampleId
   */
  def delete(sampleId: Long): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Sample
            sampleOpt <- sampleRepository.findById(SampleId(sampleId))
            sample <- Future.successful(sampleOpt.getOrElse(throw NotFoundException("Sample not found")))

            // Obtener el Resource
            resourceOpt <- resourceRepository.findById(sample.resourceId)
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

