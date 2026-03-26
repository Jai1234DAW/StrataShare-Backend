package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.Strings
import dev.pompilius.resource.domain.{Resource, ResourceAccessRepository, ResourceFilter, ResourceId, ResourceRepository, ResourceType}
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException, NotFoundException}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain.{Role, UserId}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResourceController @Inject() (
    resourceRepository: ResourceRepository,
    resourceAccessRepository: ResourceAccessRepository,
    paginatedWriter: PaginatedWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = play.api.Logger(getClass)

  /**
   * Obtiene todos los recursos con filtros opcionales y paginación
   * GET /api/resources
   */
  def listResources: Action[AnyContent] = {
    Action.async { implicit request =>
      val resourceType = request.queryString.get(Strings.resourceType).flatMap(_.headOption).flatMap(rt => scala.util.Try(ResourceType.withNameInsensitive(rt)).toOption)
      val visibility = request.queryString.get(Strings.visibility).flatMap(_.headOption).flatMap(v => scala.util.Try(Visibility.withNameInsensitive(v)).toOption)
      val localization = request.queryString.get(Strings.localization).flatMap(_.headOption)
      val page = request.queryString.get(Strings.page).flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(1)
      val pageSize = request.queryString.get(Strings.pageSize).flatMap(_.headOption).flatMap(_.toIntOption).getOrElse(10)

      val filter = ResourceFilter(
        resourceType = resourceType,
        visibility = visibility,
        localization = localization
      )

      val pagination = Pagination(page = page, pageSize = pageSize)

      for {
        resources <- resourceRepository.find(filter, pagination)
        paginatedResources = Paginated(data = resources, page = page, pageSize = pageSize, total = resources.length)
        json <- paginatedWriter.toJson(paginatedResources)
      } yield {
        Ok(json)
      }
    }
  }

  /**
   * Obtiene un recurso específico por ID
   * GET /api/resources/:resourceId
   */
  def getResource(resourceId: Long): Action[AnyContent] = {
    Action.async { implicit request =>
      for {
        resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
        resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource with id=$resourceId not found")))
        userId = getCurrentUserId

        // Validar que el usuario tenga acceso al recurso
        _ <- validateResourceAccess(resource, userId)

        json <- toJson(resource)
      } yield {
        Ok(json)
      }
    }
  }


  def createResource: Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val userId = getCurrentUserId
        val createResourceRequest = CreateResourceRequestParser.parse(request)

        val newResource = Resource(
          id = ResourceId.gen(configuration.nodeId),
          resourceType = ResourceType.withNameInsensitive(createResourceRequest.resourceType),
          ownerId = UserId(userId),
          visibility = Visibility.withNameInsensitive(createResourceRequest.visibility),
          created = clock.now,
          updated = clock.now,
          observations = createResourceRequest.observations,
          summary = createResourceRequest.summary,
          localization = createResourceRequest.localization
        )

        for {
          _ <- resourceRepository.save(newResource, None, None)
          json <- toJson(newResource)
        } yield {
          Created(json)
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error creating resource: ${e.getMessage}")
          Future.successful(BadRequest(Json.obj(Strings.error -> e.getMessage)))
      }
    }
  }

  /**
   * Actualiza un recurso existente
   * PUT /api/resources/:resourceId
   */
  def updateResource(resourceId: Long): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val userId = getCurrentUserId
        val body = request.body.asJson.getOrElse(throw BadRequestException("Invalid JSON"))

        for {
          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource with id=$resourceId not found")))

          // Validar que el usuario es el propietario
          _ <- Future.successful(
            if (resource.ownerId.id != userId) {
              throw ForbiddenException("Only the resource owner can update this resource")
            }
          )

          observations = (body \ Strings.observations).asOpt[String]
          summary = (body \ Strings.summary).asOpt[String]
          visibility = (body \ Strings.visibility).asOpt[String].map(v => Visibility.withNameInsensitive(v))

          updatedResource = resource.copy(
            observations = observations.orElse(resource.observations),
            summary = summary.orElse(resource.summary),
            visibility = visibility.getOrElse(resource.visibility),
            updated = clock.now
          )

          _ <- resourceRepository.save(updatedResource, None, None)
          json <- toJson(updatedResource)
        } yield {
          Ok(json)
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error updating resource: ${e.getMessage}")
          Future.successful(BadRequest(Json.obj(Strings.error -> e.getMessage)))
      }
    }
  }

  /**
   * Descarga un recurso (solo si el usuario tiene acceso)
   * GET /api/resources/:resourceId/download
   */
  def downloadResource(resourceId: Long): Action[AnyContent] = {
    Action.async { implicit request =>
      for {
        resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
        resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource with id=$resourceId not found")))
        userId = getCurrentUserId

        // Validar que el usuario tenga acceso
        _ <- validateResourceAccess(resource, userId)

        json <- toJson(resource)
      } yield {
        Ok(json)
      }
    }
  }

  /**
   * Elimina un recurso
   * DELETE /api/resources/:resourceId
   */
  def deleteResource(resourceId: Long): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val userId = getCurrentUserId

        for {
          resourceOpt <- resourceRepository.findById(ResourceId(resourceId))
          resource <- Future.successful(resourceOpt.getOrElse(throw NotFoundException(s"Resource with id=$resourceId not found")))

          // Validar que el usuario es el propietario
          _ <- Future.successful(
            if (resource.ownerId.id != userId) {
              throw ForbiddenException("Only the resource owner can delete this resource")
            }
          )

          _ <- resourceRepository.delete(ResourceId(resourceId))
        } yield {
          NoContent
        }
      } catch {
        case e: Exception =>
          logger.error(s"Error deleting resource: ${e.getMessage}")
          Future.successful(BadRequest(Json.obj(Strings.error -> e.getMessage)))
      }
    }
  }

  /**
   * Valida si un usuario tiene acceso a un recurso
   * - Si el recurso es público, todos tienen acceso
   * - Si es privado, solo el propietario
   * - Si es de transacción, debe estar en ResourceAccess (pagó o recibió como trueque)
   */
  private def validateResourceAccess(resource: Resource, userId: Long): Future[Unit] = {
    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(()) // Acceso público

      case Visibility.PRIVATE =>
        if (resource.ownerId.id == userId) {
          Future.successful(()) // El propietario tiene acceso
        } else {
          throw ForbiddenException("This resource is private")
        }

      case _ =>
        // Para otros tipos de visibilidad, verificar en ResourceAccess
        resourceAccessRepository.findByResourceIdAndUserId(ResourceId(resource.id.id), UserId(userId)).map {
          case Some(_) => () // Usuario tiene acceso
          case None => throw ForbiddenException("You don't have access to this resource")
        }
    }
  }

  private def toJson(resource: Resource): Future[JsValue] = {
    Future.successful(
      Json.obj(
        Strings.id -> resource.id.id.toString,
        Strings.resourceType -> resource.resourceType.toString,
        Strings.ownerId -> resource.ownerId.id.toString,
        Strings.visibility -> resource.visibility.toString,
        Strings.created -> resource.created.toString,
        Strings.updated -> resource.updated.toString,
        Strings.observations -> resource.observations,
        Strings.summary -> resource.summary,
        Strings.localization -> resource.localization
      )
    )
  }

  private def getCurrentUserId: Long = {
    getAuthenticatedUser.id.id
  }
}

