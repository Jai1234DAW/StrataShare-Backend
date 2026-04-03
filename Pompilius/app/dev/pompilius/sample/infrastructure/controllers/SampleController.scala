package dev.pompilius.sample.infrastructure.controllers

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
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.sample.infrastructure.parsers.{CreateSampleRequestParser, UpdateSampleRequestParser}
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.users.domain.{Role, UserId}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import org.joda.time.DateTime
import play.api.mvc.{Action, AnyContent}
import play.api.libs.json.Json

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SampleController @Inject() (
    sampleRepository: SampleRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceAccessValidator: ResourceAccessValidator,
    resourceWriter: ResourceWriter,
    paginatedWriter: PaginatedWriter
)(implicit val ec: ExecutionContext)
    extends BaseController {

  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createSampleRequest = CreateSampleRequestParser.parse(request)

          // Generar IDs para Resource y Sample
          val resourceId = ResourceId.gen(configuration.nodeId)
          val sampleId = SampleId.gen(configuration.nodeId)

          // Crear el Resource (datos comunes)
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

          // Crear el Sample (datos específicos)
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
            // Guarda un Resource
            _ <- resourceRepository.save(newResource)

            // Guardar en Sample
            _ <-
              sampleRepository
                .save(newSample)
                .recoverWith {
                  case NonFatal(e) =>
                    // Si falla guardar el Sample, eliminar el Resource para no dejar datos huérfanos
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
            (sample, resource) <- getSampleWithResource(sampleId)

            // Verificar que es propietario del resource y que no está borrado lógicamente
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

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
            (sample, resource) <- getSampleWithResource(sampleId)

            json <- resource.visibility match {
              // Si es PÚBLICO → Todos ven datos completos
              case Visibility.PUBLIC =>
                resourceWriter.asPublic(resource, Some(sample), None)

              // Si es PRIVADO → Verificar tipo de acceso del usuario
              case Visibility.PRIVATE =>
                resourceUserRepository.findByResourceAndUser(resource.id, user.id).flatMap {
                  case Some(ru) if !ru.deleted && ru.resourceUserType == ResourceUserType.OWNER =>
                    // Es el propietario → Vista completa con permisos
                    resourceWriter.asOwner(resource, Some(sample), None)

                  case Some(ru)
                      if !ru.deleted &&
                        (ru.resourceUserType == ResourceUserType.PURCHASED ||
                          ru.resourceUserType == ResourceUserType.ACCEPTED_AS_PAYMENT) =>
                    // Lo compró o recibió como pago → Vista completa sin permisos
                    resourceWriter.asPublic(resource, Some(sample), None)

                  case _ =>
                    // NO tiene acceso → Solo preview/teaser
                    resourceWriter.asPrivate(resource, Some(sample), None)
                }
            }

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
            (_, resource) <- getSampleWithResource(sampleId)

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
      sampleType: Option[String],
      rockType: Option[String],
      isFresh: Option[Boolean],
      visibility: Option[String],
      localization: Option[String],
      userId: Option[String],
      pag: Pagination
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            samples <- sampleRepository.find(
              SampleFilter(
                name = name,
                sampleType = sampleType,
                rockType = rockType,
                isFresh = isFresh,
                visibility = visibility.map(Visibility.withNameInsensitive),
                localization = localization,
                userId = userId.map(UserId(_))
              ),
              pag.oneMore
            )
            // 2. Para cada study, obtener resource y generar preview JSON usando paginatedWriter
            json <- paginatedWriter.toJson(Paginated(samples, pag)) { sample =>
              for {
                resource <-
                  resourceRepository
                    .findById(sample.resourceId)
                    .map(
                      _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study ${sample.id}"))
                    )
                // Siempre devolver preview (datos básicos para el listado)
                //Luego se pordrá acceder con más datos a cada uno de ellos.
                json <- resourceWriter.asPrivate(resource, Some(sample), None)
              } yield json
            }

          } yield {
            Ok(json)
          }
      }
    }
  private def getSampleWithResource(sampleId: String): Future[(Sample, Resource)] = {
    for {
      sample <-
        sampleRepository
          .findById(SampleId(sampleId))
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Sample with id $sampleId not found"))
          )

      resource <-
        resourceRepository
          .findById(sample.resourceId)
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $sampleId"))
          )
    } yield (sample, resource)
  }
}
