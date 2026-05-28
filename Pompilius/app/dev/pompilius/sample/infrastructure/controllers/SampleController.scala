package dev.pompilius.sample.infrastructure.controllers

import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.badge.application.BadgeService
import dev.pompilius.event.domain.EventU
import dev.pompilius.resource.domain._
import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.sample.infrastructure.parsers.{CreateSampleRequestParser, UpdateSampleRequestParser}
import dev.pompilius.shared.domain.{Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain.{Role, UserId}
import play.api.Logger
import play.api.libs.Files
import play.api.mvc.{Action, AnyContent, MultipartFormData}

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
    extends BaseController
    with Attachments {

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
            price = createSampleRequest.price,
            isBarter = createSampleRequest.isBarter
          )

          // Crear el Sample (datos específicos)
          val newSample = Sample(
            id = sampleId,
            resourceId = resourceId,
            collectedDate = createSampleRequest.collectedDate,
            minerals = createSampleRequest.minerals,
            collectionMethods = createSampleRequest.collectionMethods,
            isFresh = createSampleRequest.isFresh,
            sampleType = createSampleRequest.sampleType,
            materialsUsed = createSampleRequest.materialsUsed,
            sampleCategory = createSampleRequest.sampleCategory,
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
                created = clock.now,
                updated = clock.now
              )
            )

            // Registrar evento para badges.
            samplesBadges <- badgeService.registerEventAndCheckBadges(user.id, EventU.SAMPLE_UPLOADED)

            _ = if (samplesBadges.nonEmpty) {
              logger.info(
                s"Buyer ${user.id} earned ${samplesBadges.length} badge(s) after barter: ${samplesBadges.map(_.name).mkString(", ")}"
              )
            }

            // 4. Retornar JSON
            json <- resourceWriter.asPublic(newResource, ResourceAccessLevel.OWNER, user.id, None, Some(newSample), None)
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
              price = updateSampleRequest.price.orElse(resource.price),
              isBarter = updateSampleRequest.isBarter.getOrElse(resource.isBarter),
              updated = clock.now
            )

            // Actualizar el Sample (datos específicos)
            updatedSample = sample.copy(
              minerals = updateSampleRequest.minerals.orElse(sample.minerals),
              collectionMethods = updateSampleRequest.collectionMethods.orElse(sample.collectionMethods),
              isFresh = updateSampleRequest.isFresh.getOrElse(sample.isFresh),
              sampleType = updateSampleRequest.sampleType.orElse(sample.sampleType),
              materialsUsed = updateSampleRequest.materialsUsed.orElse(sample.materialsUsed),
              sampleCategory = updateSampleRequest.sampleCategory.orElse(sample.sampleCategory),
              geologicalProcesses = updateSampleRequest.geologicalProcesses.orElse(sample.geologicalProcesses)
            )

            // Guardar cambios
            _ <- resourceRepository.save(updatedResource)
            _ <- sampleRepository.save(updatedSample)

            // Retornar JSON actualizado
            json <-
              resourceWriter.asPublic(updatedResource, ResourceAccessLevel.OWNER, user.id, None, Some(updatedSample), None)
          } yield {
            Ok(json)
          }
      }
    }

  def createWithAttachments: Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createSampleRequest =
            CreateSampleRequestParser.parseMultipart(request.body)

          val files = request.body.files

          val resourceId = ResourceId.gen(configuration.nodeId)
          val sampleId = SampleId.gen(configuration.nodeId)

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
            price = createSampleRequest.price,
            isBarter = createSampleRequest.isBarter
          )

          val newSample = Sample(
            id = sampleId,
            resourceId = resourceId,
            collectedDate = createSampleRequest.collectedDate,
            minerals = createSampleRequest.minerals,
            collectionMethods = createSampleRequest.collectionMethods,
            isFresh = createSampleRequest.isFresh,
            sampleType = createSampleRequest.sampleType,
            materialsUsed = createSampleRequest.materialsUsed,
            sampleCategory = createSampleRequest.sampleCategory,
            geologicalProcesses = createSampleRequest.geologicalProcesses
          )

          for {
            _ <- resourceRepository.save(newResource)

            _ <- sampleRepository.save(newSample).recoverWith {
              case NonFatal(e) =>
                resourceRepository.delete(resourceId).map(_ => throw e)
            }

            _ <- resourceUserRepository.save(
              ResourceUser(
                resourceId = resourceId,
                userId = user.id,
                resourceUserType = ResourceUserType.OWNER,
                created = clock.now,
                updated = clock.now
              )
            )
            attachments <- Future.sequence {
              request.body.files.map { filePart =>
                saveAsAttachment(
                  user = user,
                  id = None,
                  resourceId = Some(resourceId),
                  file = filePart.ref.path.toFile,
                  originalFilename = filePart.filename,
                  description = filePart.contentType.map { ct =>
                    if (ct.startsWith("image/")) "image" else "file"
                  },
                  contentType = filePart.contentType
                )
              }
            }

            samplesBadges <- badgeService.registerEventAndCheckBadges(user.id, EventU.SAMPLE_UPLOADED)

            _ = if (samplesBadges.nonEmpty) {
              logger.info(
                s"User ${user.id} earned ${samplesBadges.length} badge(s): ${samplesBadges.map(_.name).mkString(", ")}"
              )
            }

            json <- resourceWriter.asPublic(newResource, ResourceAccessLevel.OWNER, user.id, None, Some(newSample), None)

          } yield Ok(json)
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

            ownerId <-
              resourceUserRepository
                .findOwnerByResource(resource.id)
                .map(
                  _.map(_.id).getOrElse(
                    throw new ResourceNotFoundException(s"Owner not found for resource ${resource.id}")
                  )
                )

            json <- accessLevel match {
              case ResourceAccessLevel.FULL_ACCESS =>

                for {
                  resourceUserOpt <- resourceUserRepository.findByResourceAndUser(resource.id, user.id)
                  userTypeRelation = resourceUserOpt.map(_.resourceUserType.toString)
                  baseJson <- resourceWriter.asPublic(resource, accessLevel, ownerId, userTypeRelation, Some(sample), None)
                } yield baseJson

              case ResourceAccessLevel.OWNER =>
                resourceWriter.asPublic(resource, accessLevel, ownerId, Some(ResourceUserType.OWNER.toString), Some(sample), None)

              case _ =>
                // PREVIEW_ONLY → Solo preview
                resourceWriter.asPrivate(resource, accessLevel, ownerId, Some(sample), None)

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
            _ <- resourceUserRepository.deleteRelation(resource.id, user.id)

          } yield {
            Ok
          }
      }
    }

  def getAll(
      name: Option[String],
      sampleType: Option[String],
      sampleCategory: Option[String],
      isFresh: Option[Boolean],
      visibility: Option[String],
      location: Option[String],
      search: Option[String],
      userId: Option[String],
      pag: Pagination
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            // 1. Obtener samples con los filtros (incluye los del usuario)
            samples <- sampleRepository.find(
              SampleFilter(
                name = name,
                sampleType = sampleType,
                sampleCategory = sampleCategory,
                isFresh = isFresh,
                visibility = visibility.map(Visibility.withNameInsensitive),
                location = location,
                search = search,
                userId = userId.map(UserId(_))
              ),
              pag.oneMore
            )

            // 2. Obtener IDs de samples OWNED por el usuario autenticado para excluirlos
            userOwnedSampleIds <- sampleRepository.getMyAllSamplesAs(
              userId = user.id,
              Pagination.all,
              ResourceUserType.OWNER.toString
            ).map(_.map(_.id).toSet)

            // `moreItems` debe basarse en el resultado paginado del repo (antes de excluir OWNED)
            hasMore = pag.limit.exists(limit => samples.length > limit)

            // 3. EXCLUIR SOLO samples que el usuario OWNS (no excluir los comprados, barteados, etc.)
            filteredSamples = samples.filterNot(s => userOwnedSampleIds.contains(s.id))
            visibleSamples = pag.limit.map(limit => filteredSamples.take(limit)).getOrElse(filteredSamples)

            // 4. Para cada sample, obtener resource y generar preview JSON usando paginatedWriter
            json <- paginatedWriter.toJson(Paginated(visibleSamples, hasMore)) { sample =>
              for {
                resource <-
                  resourceRepository
                    .findById(sample.resourceId)
                    .map(
                      _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study ${sample.id}"))
                    )

                ownerId <-
                  resourceUserRepository
                    .findOwnerByResource(resource.id)
                    .map(
                      _.map(_.id).getOrElse(
                        throw new ResourceNotFoundException(s"Owner not found for resource ${resource.id}")
                      )
                    )

                // Siempre devolver preview (datos básicos para el listado)
                json <-
                  resourceWriter.asPrivate(resource, ResourceAccessLevel.PREVIEW_ONLY, ownerId, Some(sample), None)
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

  def getAllMySamples(
      pag: Pagination,
      userType: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            samples <- userType.toUpperCase.replace(" ","_") match {
              case "OWNER" =>
                sampleRepository.getMyAllSamplesAs(userId = user.id, pag.oneMore, ResourceUserType.OWNER.toString)
              case "ACCEPTED_AS_PAYMENT" =>
                sampleRepository.getMyAllSamplesAs(userId = user.id, pag.oneMore, ResourceUserType.ACCEPTED_AS_PAYMENT.toString)

              case "PURCHASED" =>
                sampleRepository.getMyAllSamplesAs(userId = user.id, pag.oneMore, ResourceUserType.PURCHASED.toString)

                case "BARTERED" =>
                sampleRepository.getMyAllSamplesAs(userId = user.id, pag.oneMore, ResourceUserType.BARTERED.toString)

                case _ =>
                Future.failed(new IllegalArgumentException(s"Invalid userType: $userType"))
            }

            json <- paginatedWriter.toJson(Paginated(samples, pag)) { sample =>
              for {
                resource <-
                  resourceRepository
                    .findById(sample.resourceId)
                    .map(
                      _.getOrElse(
                        throw new ResourceNotFoundException(
                          s"Resource not found for sample ${sample.id}"
                        )
                      )
                    )

                json <- resourceWriter.asPublic(resource, ResourceAccessLevel.OWNER, user.id, None, Some(sample), None)
              } yield json
            }
          } yield Ok(json)
      }
    }

  def getAllAcquiredSamples(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {

            // Obtener muestras compradas
            purchasedSamples <- sampleRepository.getMyAllSamplesAs(
              userId = user.id,
              pag.oneMore,
              ResourceUserType.PURCHASED.toString
            )

            // Obtener muestras conseguidas por barter (aceptadas como pago)
            acceptedSamples <- sampleRepository.getMyAllSamplesAs(
              userId = user.id,
              pag.oneMore,
              ResourceUserType.ACCEPTED_AS_PAYMENT.toString
            )

            barteredSamples <- sampleRepository.getMyAllSamplesAs(
              userId = user.id,
              pag.oneMore,
              ResourceUserType.BARTERED.toString
            )
            allSamples = (purchasedSamples ++ barteredSamples ++ acceptedSamples).distinct

            json <- paginatedWriter.toJson(Paginated(allSamples, pag)) { sample =>
              for {
                resource <-
                  resourceRepository
                    .findById(sample.resourceId)
                    .map(
                      _.getOrElse(
                        throw new ResourceNotFoundException(
                          s"Resource not found for sample ${sample.id}"
                        )
                      )
                    )

                json <- resourceWriter.asPublic(resource, ResourceAccessLevel.FULL_ACCESS, user.id, None, Some(sample), None)
              } yield json
            }
          } yield Ok(json)
      }
    }
}
