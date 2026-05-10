package dev.pompilius.study.infrastructure.controllers

import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.badge.application.BadgeService
import dev.pompilius.event.domain.EventU
import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain._
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.resource.infrastructure.writers.ResourceWriter
import dev.pompilius.sample.domain.{SampleId, SampleRepository}
import dev.pompilius.shared.domain.exceptions.ForbiddenException
import dev.pompilius.shared.domain.{Clock, Paginated, Pagination, Visibility}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.study.domain._
import dev.pompilius.study.infrastructure.parsers.{AddSamplesToStudyRequestParser, CreateStudyRequestParser, UpdateStudyRequestParser}
import dev.pompilius.study.infrastructure.writers.StudySampleWriter
import dev.pompilius.users.domain.{Role, UserId}
import play.api.Logger
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MultipartFormData}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    studySampleRepository: StudySampleRepository,
    sampleRepository: SampleRepository,
    resourceRepository: ResourceRepository,
    resourceUserRepository: ResourceUserRepository,
    resourceWriter: ResourceWriter,
    resourceAccessValidator: ResourceAccessValidator,
    paginatedWriter: PaginatedWriter,
    studySampleWriter: StudySampleWriter,
    badgeService: BadgeService,
    clock: Clock
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {
  private val logger = Logger(this.getClass)

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
            name = createStudyRequest.name,
            resourceType = ResourceType.STUDY,
            visibility = createStudyRequest.visibility,
            created = clock.now,
            updated = clock.now,
            location = createStudyRequest.location,
            observations = createStudyRequest.observations,
            summary = createStudyRequest.summary,
            price = createStudyRequest.price,
            isBarter = createStudyRequest.isBarter
          )

          // Crear el Study (datos específicos)
          val newStudy = Study(
            id = studyId,
            resourceId = resourceId,
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
                created = clock.now,
                updated = clock.now
              )
            )

            json <- resourceWriter.asPublic(newResource, ResourceAccessLevel.OWNER, user.id, None, Some(newStudy))

            //Llamo aquí a lo de eventos.
            // Registrar evento
            studiesBadges <- badgeService.registerEventAndCheckBadges(user.id, EventU.STUDY_UPLOADED)

            _ = if (studiesBadges.nonEmpty) {
              logger.info(
                s"Buyer ${user.id} earned ${studiesBadges.length} badge(s) after barter: ${studiesBadges.map(_.name).mkString(", ")}"
              )
            }
          } yield {
            Ok(json)
          }
      }
    }

  def createWithAttachments: Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createStudyRequest =
            CreateStudyRequestParser.parseMultipart(request.body)

          val files = request.body.files

          val resourceId = ResourceId.gen(configuration.nodeId)
          val studyId = StudyId.gen(configuration.nodeId)

          val newResource = Resource(
            id = resourceId,
            name = createStudyRequest.name,
            resourceType = ResourceType.STUDY,
            visibility = createStudyRequest.visibility,
            created = clock.now,
            updated = clock.now,
            location = createStudyRequest.location,
            observations = createStudyRequest.observations,
            summary = createStudyRequest.summary,
            price = createStudyRequest.price,
            isBarter = createStudyRequest.isBarter
          )

          // Crear el Study (datos específicos)
          val newStudy = Study(
            id = studyId,
            resourceId = resourceId,
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

            studyBadges <- badgeService.registerEventAndCheckBadges(user.id, EventU.STUDY_UPLOADED)

            _ = if (studyBadges.nonEmpty) {
              logger.info(
                s"User ${user.id} earned ${studyBadges.length} badge(s): ${studyBadges.map(_.name).mkString(", ")}"
              )
            }

            json <- resourceWriter.asPublic(newResource, ResourceAccessLevel.OWNER, user.id, None, Some(newStudy))

          } yield Ok(json)
      }
    }

  def update(studyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val updateStudyRequest = UpdateStudyRequestParser.parse(request)

          for {

            (study, resource) <- getStudyWithResource(studyId)

            // Verificar que es propietario del resource y que no está borrado lógicamente
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

            updatedResource = resource.copy(
              name = updateStudyRequest.name.getOrElse(resource.name),
              visibility = updateStudyRequest.visibility.getOrElse(resource.visibility),
              location = updateStudyRequest.location.getOrElse(resource.location),
              observations = updateStudyRequest.observations.orElse(resource.observations),
              summary = updateStudyRequest.summary.orElse(resource.summary),
              price = updateStudyRequest.price.orElse(resource.price),
              isBarter = updateStudyRequest.isBarter.getOrElse(resource.isBarter),
              updated = clock.now
            )

            // Actualizar el Study (datos específicos)
            updatedStudy = study.copy(
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
            json <-
              resourceWriter.asPublic(updatedResource, ResourceAccessLevel.OWNER, user.id, None, Some(updatedStudy))
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
              case ResourceAccessLevel.OWNER =>
                resourceWriter.asPublic(resource, accessLevel, ownerId, None, Some(study))

              case ResourceAccessLevel.FULL_ACCESS =>
                resourceWriter.asPublic(resource, accessLevel, ownerId, None, Some(study))

              case _ =>
                // PREVIEW_ONLY → Solo preview
                resourceWriter.asPrivate(resource, accessLevel, ownerId, None, Some(study))
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
            _ <- resourceUserRepository.deleteRelation(resource.id, user.id)
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
      visibility: Option[String],
      location: Option[String],
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
                year = year,
                area = area.map(Area.withNameInsensitive),
                authors = authors,
                search = search,
                visibility = visibility.map(Visibility.withNameInsensitive),
                location = location,
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

//                ownerId <-
//                  resourceUserRepository
//                    .findOwnerByResource(resource.id)
//                    .map(
//                      _.map(_.id).getOrElse(
//                        throw new ResourceNotFoundException(s"Owner not found for resource ${resource.id}")
//                      )
//                    )

                json <-
                  resourceWriter
                    .asPrivate(resource, ResourceAccessLevel.PREVIEW_ONLY, currentUser.id, None, Some(study))
              } yield json
            }

          } yield {
            Ok(json)
          }
      }
    }

  def getAllMyStudies(
      pag: Pagination,
      userType: String
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {

            studies <- userType.toUpperCase.replace(" ", "_") match {
              case "OWNER" =>
                studyRepository.getMyAllStudiesAs(userId = user.id, pag.oneMore, ResourceUserType.OWNER.toString)
              case "ACCEPTED_AS_PAYMENT" =>
                studyRepository
                  .getMyAllStudiesAs(userId = user.id, pag.oneMore, ResourceUserType.ACCEPTED_AS_PAYMENT.toString)

              case "PURCHASED" =>
                studyRepository.getMyAllStudiesAs(userId = user.id, pag.oneMore, ResourceUserType.PURCHASED.toString)

              case "BARTERED" =>
                studyRepository.getMyAllStudiesAs(userId = user.id, pag.oneMore, ResourceUserType.BARTERED.toString)

              case _ =>
              Future.failed(new IllegalArgumentException("Invalid userType. Must be one of: OWNER, ACCEPTED_AS_PAYMENT, PURCHASED, BARTERED"))
            }

            json <- paginatedWriter.toJson(Paginated(studies, pag)) { study =>
              for {
                resource <-
                  resourceRepository
                    .findById(study.resourceId)
                    .map(
                      _.getOrElse(
                        throw new ResourceNotFoundException(
                          s"Resource not found for study ${study.id}"
                        )
                      )
                    )

                json <- resourceWriter.asPublic(resource, ResourceAccessLevel.OWNER, user.id, None, Some(study))
              } yield json
            }
          } yield Ok(json)
      }
    }

  //Para añadir muestras a un estudio
  def addSamples(studyId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val sid = StudyId(studyId)

          for {
            // Verificar que el estudio existe
            (_, resource) <- getStudyWithResource(studyId)

            // Verificar que es propietario del estudio
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

            // Parsear el request
            addSamplesRequest = AddSamplesToStudyRequestParser.parse(request)

            // Verificar que todas las muestras existen Y son del usuario
            _ <- Future.sequence(addSamplesRequest.sampleIds.map { sampleId =>
              for {
                // Verificar que la muestra existe
                sample <-
                  sampleRepository
                    .findById(sampleId)
                    .map(_.getOrElse(throw new ResourceNotFoundException(s"Sample $sampleId not found")))

                // Obtener el recurso asociado a la muestra
                sampleResource <-
                  resourceRepository
                    .findById(sample.resourceId)
                    .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource not found for sample $sampleId")))

                // Verificar que el usuario es propietario de la muestra
                _ <- resourceAccessValidator.verifyOwnership(sampleResource.id, user.id)
              } yield ()
            })

            // Crear las relaciones
            studySamples = addSamplesRequest.sampleIds.map(sampleId => StudySample(sid, sampleId))
            _ <- studySampleRepository.saveMultiple(studySamples)

          } yield {
            Ok
          }
      }
    }

  def removeSample(studyId: String, sampleId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val sid = StudyId(studyId)
          val sampId = SampleId(sampleId)

          for {
            // Verificar que el estudio existe
            (_, resource) <- getStudyWithResource(studyId)

            // Verificar que es propietario
            _ <- resourceAccessValidator.verifyOwnership(resource.id, user.id)

            // Verificar que la muestra existe
            _ <-
              sampleRepository
                .findById(sampId)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Sample $sampleId not found")))

            // Eliminar la relación
            _ <- studySampleRepository.delete(sid, sampId)

          } yield {
            Ok
          }
      }
    }

  def getSamples(studyId: String, pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val sid = StudyId(studyId)

          for {
            // Verificar que el estudio existe
            (study, resource) <- getStudyWithResource(studyId)

            // Validar acceso - solo pueden ver muestras si tienen acceso completo
            accessLevel <- resourceAccessValidator.getAccessLevel(resource.id, user.id)
            _ <- accessLevel match {
              case ResourceAccessLevel.OWNER =>
                Future.successful(())

              case ResourceAccessLevel.FULL_ACCESS =>
                Future.successful(())

              case _ =>
                // Solo tiene preview, no puede ver las muestras asociadas
                Future.failed(
                  new ForbiddenException("You need to purchase this study to view associated samples")
                )
            }

            // Obtener relaciones study-sample
            studySamples <- studySampleRepository.getAllByStudyId(sid)

            // Usar paginatedWriter para generar JSON paginado
              json <- paginatedWriter.toJson(Paginated(studySamples, pag)) { studySample =>
              Future.successful(Json.toJson(studySample.sampleId.toString))

            }
          } yield {
            Ok(json)
          }
      }
    }

  private def getStudyWithResource(studyId: String): Future[(Study, Resource)] = {
    val sid = StudyId(studyId)
    for {
      study <-
        studyRepository
          .findById(sid)
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Study with id $studyId not found"))
          )

      resource <-
        resourceRepository
          .findById(study.resourceId)
          .map(
            _.getOrElse(throw new ResourceNotFoundException(s"Resource not found for study $studyId"))
          )
    } yield (study, resource)
  }

}
