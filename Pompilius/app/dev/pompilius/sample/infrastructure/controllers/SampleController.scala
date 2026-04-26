package dev.pompilius.sample.infrastructure.controllers

import dev.pompilius.badge.application.BadgeService
import dev.pompilius.event.domain.EventU
import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain._
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.sample.infrastructure.parsers.{CreateSampleRequestParser, UpdateSampleRequestParser}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain.{Role, UserId}
import play.api.Logger
import play.api.mvc.{Action, AnyContent}

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
    paginatedWriter: PaginatedWriter,
    badgeService: BadgeService
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

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
            name = createSampleRequest.name,
            resourceType = ResourceType.SAMPLE,
            visibility = createSampleRequest.visibility,
            created = clock.now,
            updated = clock.now,
            location = createSampleRequest.location,
            observations = createSampleRequest.observations,
            summary = createSampleRequest.summary,
            price=createSampleRequest.price,
            isBarter=createSampleRequest.isBarter
          )

          // Crear el Sample (datos específicos)
          val newSample = Sample(
            id = sampleId,
            resourceId = resourceId,
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
            //Llamo aquí a lo de eventos.
            // Registrar evento
            samplesBadges <- badgeService.registerEventAndCheckBadges(user.id, EventU.SAMPLE_UPLOADED)

            _ = if (samplesBadges.nonEmpty) {
              logger.info(
                s"Buyer ${user.id} earned ${samplesBadges.length} badge(s) after barter: ${samplesBadges.map(_.name).mkString(", ")}"
              )
            }

            // 4. Retornar JSON
            json <- resourceWriter.asPublic(newResource, Some(newSample), None)
          } yield {
            Ok(json)
          }
      }
    }

  def update(sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val sid = SampleId(sampleId)
          val updateSampleRequest = UpdateSampleRequestParser.parse(request)

          for {
            (sample, resource) <- getSampleWithResource(sid)

            // Verificar que es propietario del resource y que no está borrado lógicamente
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

            // Actualizar el Resource (datos comunes)
            updatedResource = resource.copy(
              name = updateSampleRequest.name.getOrElse(resource.name),
              visibility = updateSampleRequest.visibility.getOrElse(resource.visibility),
              location = updateSampleRequest.location.getOrElse(resource.location),
              observations = updateSampleRequest.observations.orElse(resource.observations),
              summary = updateSampleRequest.summary.orElse(resource.summary),
              updated = clock.now
            )

            // Actualizar el Sample (datos específicos)
            updatedSample = sample.copy(
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
            json <- resourceWriter.asPublic(updatedResource, Some(updatedSample), None)
          } yield {
            Ok(json)
          }
      }
    }

  def get(sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val sid = SampleId(sampleId)

          for {
            (sample, resource) <- getSampleWithResource(sid)

            // Obtener el nivel de acceso del usuario
            accessLevel <- resourceAccessValidator.getAccessLevel(resource.id, user.id)

            json <- accessLevel match {
              case ResourceAccessLevel.FULL_ACCESS =>
                resourceWriter.asPublic(resource, Some(sample), None)

              case _ =>
                // PREVIEW_ONLY → Solo preview
                resourceWriter.asPrivate(resource, Some(sample), None)

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
            (_, resource) <- getSampleWithResource(SampleId(sampleId))

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
      location: Option[String],
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
                location = location,
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
                //Luego se pordá acceder con más datos a cada uno de ellos.
                json <- resourceWriter.asPrivate(resource, Some(sample), None)
              } yield json
            }

          } yield {
            Ok(json)
          }
      }
    }

  private def getSampleWithResource(sampleId: SampleId): Future[(Sample, Resource)] = {
    for {

      sample <-
        sampleRepository
          .findById(sampleId)
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
