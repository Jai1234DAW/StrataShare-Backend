package dev.pompilius.sample.infrastructure.controllers

import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceRepository, ResourceType, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.sample.domain.{Sample, SampleId, SampleRepository}
import dev.pompilius.sample.infrastructure.parsers.{CreateSampleRequestParser, UpdateSampleRequestParser}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.domain.{Pagination, Visibility}
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class SampleController @Inject() (
    sampleRepository: SampleRepository,
    resourceRepository: ResourceRepository,
    userResourceRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {


  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createSampleRequest = CreateSampleRequestParser.parse(request)

          // Generar IDs
          val resourceId = ResourceId.gen(configuration.nodeId)
          val sampleId = SampleId.gen(configuration.nodeId)

          // 1. Crear el Resource (datos comunes)
          val newResource = Resource(
            id = resourceId,
            resourceType = ResourceType.SAMPLE,
            visibility = createSampleRequest.visibility,
            created = clock.now,
            updated = clock.now,
            localization = createSampleRequest.localization,
            observations = createSampleRequest.observations,
            summary = createSampleRequest.summary
          )

          // 2. Crear el Sample (datos específicos)
          val newSample = Sample(
            id = sampleId,
            resourceId = resourceId,
            name = createSampleRequest.name,
            minerals = createSampleRequest.minerals,
            collectionMethods = createSampleRequest.collectionMethods,
            isFresh = createSampleRequest.isFresh,
            sampleType = createSampleRequest.sampleType,
            materialsUsed = createSampleRequest.materialsUsed,
            rockType = createSampleRequest.rockType,
            geologicalProcesses = createSampleRequest.geologicalProcesses
          )

          for {
            // 1. Guardar Resource
            _ <- resourceRepository.save(newResource)

            // 2. Guardar Sample
            _ <- sampleRepository.save(newSample)

            // 3. Vincular a usuario
            _ <- userResourceRepository.save(
              ResourceUser(
                resourceId = resourceId,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now
              )
            )
            // 4. Retornar JSON
            json <- resourceWriter.asOwner(newResource, Some(newSample), None)
          } yield {
            Ok(json)
          }
      }
    }

  def update(sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val updateSampleRequest = UpdateSampleRequestParser.parse(request)

          for {
            // Obtener el Sample
            sample <- sampleRepository.findById(SampleId(sampleId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Sample with id $sampleId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(sample.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $sampleId"))
            )

            // Verificar que es propietario del resource
            _ <- verifyOwnership(resource.id, user.id)

            // Actualizar el Resource (datos comunes)
            updatedResource = resource.copy(
              visibility = updateSampleRequest.visibility.getOrElse(resource.visibility),
              localization = updateSampleRequest.localization.getOrElse(resource.localization),
              observations = updateSampleRequest.observations.orElse(resource.observations),
              summary = updateSampleRequest.summary.orElse(resource.summary),
              updated = clock.now
            )

            // Actualizar el Sample (datos específicos)
            updatedSample = sample.copy(
              name = updateSampleRequest.name.getOrElse(sample.name),
              minerals = updateSampleRequest.minerals.orElse(sample.minerals),
              collectionMethods = updateSampleRequest.collectionMethods.orElse(sample.collectionMethods),
              isFresh = updateSampleRequest.isFresh.getOrElse(sample.isFresh),
              sampleType = updateSampleRequest.sampleType.orElse(sample.sampleType),
              materialsUsed = updateSampleRequest.materialsUsed.orElse(sample.materialsUsed),
              rockType = updateSampleRequest.rockType.orElse(sample.rockType),
              geologicalProcesses = updateSampleRequest.geologicalProcesses.orElse(sample.geologicalProcesses)
            )

            // Guardar cambios
            _ <- resourceRepository.save(updatedResource)
            _ <- sampleRepository.save(updatedSample)

            // Retornar JSON actualizado
            json <- resourceWriter.asOwner(updatedResource, Some(updatedSample), None)
          } yield {
            Ok(json)
          }
      }
    }


  def get(sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            // Obtener el Sample
            sample <- sampleRepository.findById(SampleId(sampleId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Sample with id $sampleId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(sample.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $sampleId"))
            )

            // Validar acceso según visibilidad
            _ <- validateAccess(resource, user.id)

            // Retornar JSON del sample completo
            json <- resourceWriter.asPublic(resource, Some(sample), None)
          } yield {
            Ok(json)
          }
      }
    }

  def delete(sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            // Obtener el Sample
            sample <- sampleRepository.findById(SampleId(sampleId)).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Sample with id $sampleId not found"))
            )

            // Obtener el Resource asociado
            resource <- resourceRepository.findById(sample.resourceId).map(
              _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $sampleId"))
            )

            // Verificar que es propietario
            _ <- verifyOwnership(resource.id, user.id)

            // Marcar ResourceUser como eliminado (soft delete)
            _ <- userResourceRepository.save(
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
            Ok(Json.obj("message" -> "Sample deleted successfully"))
          }
      }
    }

  private def verifyOwnership(resourceId: ResourceId, userId: UserId): Future[Unit] = {
    userResourceRepository.findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all).map { resourceIds =>
      if (resourceIds.contains(resourceId)) ()
      else throw new Exception("You don't have permission to update this resource")
    }
  }

  private def validateAccess(resource: Resource, userId: UserId): Future[Unit] = {
    import dev.pompilius.shared.domain.Visibility

    resource.visibility match {
      case Visibility.PUBLIC =>
        Future.successful(())

      case Visibility.PRIVATE =>
        // Privado: solo el propietario puede acceder
        userResourceRepository.findByUserAndType(userId, ResourceUserType.OWNER, Pagination.all).map { resourceIds =>
          if (resourceIds.contains(resource.id)) {
            // Verificar que el ResourceUser no esté marcado como deleted
            // Para esto, necesitamos buscar el ResourceUser específico
            ()
          } else {
            throw new Exception("You don't have permission to access this private resource")
          }
        }
    }
  }
}