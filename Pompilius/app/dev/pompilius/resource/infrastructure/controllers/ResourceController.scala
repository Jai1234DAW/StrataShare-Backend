package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.attachment.domain.{Attachment, AttachmentId, AttachmentRepository}
import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.attachment.infrastructure.writers.AttachmentWriter
import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{ResourceId, ResourceRepository}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.Role
import play.api.libs.Files
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MultipartFormData}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ResourceController @Inject() (
    resourceRepository: ResourceRepository,
    attachmentRepository: AttachmentRepository,
    resourceAccessValidator: ResourceAccessValidator,
    attachmentWriter: AttachmentWriter
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {

  /**
    * Sube múltiples archivos a un recurso
    * POST /api/resources/:resourceId/files
    */
  def uploadFiles(resourceId: String): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body
          val rid = ResourceId(resourceId)

          for {
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            _ <-
              if (request.body.files.isEmpty)
                Future.failed(new BadRequestException("No files uploaded"))
              else
                Future.unit

            _ <- resourceAccessValidator.verifyOwnership(rid, user.id)
            attachments <- uploadMultipleFiles(user, body, rid)
            response <- attachmentWriter.asList(attachments.toList)

          } yield Ok(response)
      }
    }

  /**
    * Sube múltiples imágenes con redimensionamiento a un recurso
    * POST /api/resources/:resourceId/files/images
    */
  def uploadImages(resourceId: String): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.PROFESSIONAL, Role.STUDENT)) {
        case (_, user, _, _) =>
          val rid = ResourceId(resourceId)
          val body = request.body

          // Filtrar solo imágenes
          val imageFiles = body.files.filter(_.contentType.exists(_.startsWith("image/")))

          if (imageFiles.isEmpty) {
            Future.failed(new BadRequestException("No image files uploaded"))
          } else {
            for {
              _ <-
                resourceRepository
                  .findById(rid)
                  .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource not found")))

              _ <- resourceAccessValidator.verifyOwnership(rid, user.id)

              attachments <- Future.sequence {
                imageFiles.map { file =>
                  val singleBody = MultipartFormData(
                    dataParts = body.dataParts,
                    files = Seq(file),
                    badParts = Seq.empty
                  )

                  uploadImage(
                    user = user,
                    body = singleBody,
                    maxWidth = 1920,
                    maxHeight = 1080
                  ).map { attachment =>
                    // Asociar el attachment al resourceId
                    val updated = attachment.copy(resourceId = Some(rid))
                    attachmentRepository.save(updated).map(_ => updated)
                  }.flatten
                }
              }

              response <- attachmentWriter.asList(attachments.toList)

            } yield Ok(response)
          }
      }
    }

  /**
    * Descarga un archivo
    * GET /api/resources/files/:attachmentId
    */
  def downloadFile(attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            attachment <-
              attachmentRepository
                .findById(AttachmentId(attachmentId))
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Attachment $attachmentId not found")))

            resId <- Future.successful(attachment.resourceId match {
              case Some(id: ResourceId) => id
              case None                 => throw new BadRequestException("This attachment is not associated with a resource")
            })

            _ <- validateAccess(resId, user.id)
            result <- doDownload(Some(user), attachment)

          } yield result
      }
    }

  /**
    * Elimina un archivo
    * DELETE /api/resources/files/:attachmentId
    */
  def deleteFile(attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            attachment <-
              attachmentRepository
                .findById(AttachmentId(attachmentId))
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Attachment $attachmentId not found")))

            resId <- Future.successful(attachment.resourceId match {
              case Some(id: ResourceId) => id
              case None                 => throw new BadRequestException("This attachment is not associated with a resource")
            })

            _ <- resourceAccessValidator.verifyOwnership(resId, user.id)
            _ <- attachmentRepository.delete(attachment.id)

          } yield {
            Ok(Json.obj("message" -> "File deleted successfully"))
          }
      }
    }

  /**
    * Lista archivos de un recurso
    * GET /api/resources/:resourceId/files
    */
  def listFiles(resourceId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val resId = ResourceId(resourceId)

          for {
            _ <-
              resourceRepository
                .findById(resId)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $resourceId not found")))

            _ <- validateAccess(resId, user.id)
            attachments <- attachmentRepository.findByResourceId(resId)
            filteredAttachments = attachments.filter(!_.deleted)
            response <- attachmentWriter.asList(filteredAttachments)

          } yield Ok(response)
      }
    }

  /**
    * Obtiene información de un archivo específico por ID
    * GET /api/resources/files/:attachmentId/info
    */
  def getFile(attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          attachmentRepository.findById(AttachmentId(attachmentId)).flatMap {
            case Some(attachment) =>
              attachment.resourceId match {
                case Some(resId) =>
                  validateAccess(resId, user.id).flatMap { _ =>
                    attachmentWriter.asCurrentUser(attachment).map(json => Ok(json))
                  }
                case None =>
                  Future.failed(new BadRequestException("This attachment is not associated with a resource"))
              }
            case None =>
              Future.failed(new ResourceNotFoundException(s"Attachment $attachmentId not found"))
          }
      }
    }

  /**
    * Actualiza la metadata de un attachment (descripción, filename)
    * PUT /api/resources/files/:attachmentId/metadata
    */
  def updateFileMetadata(attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body.asJson.getOrElse(throw new BadRequestException("JSON body expected"))

          val newDescription = (body \ "description").asOpt[String]
          val newFilename = (body \ "filename").asOpt[String]

          for {
            attachment <-
              attachmentRepository
                .findById(AttachmentId(attachmentId))
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Attachment not found")))

            resId <- Future.successful(attachment.resourceId match {
              case Some(id: ResourceId) => id
              case None                 => throw new BadRequestException("This attachment is not associated with a resource")
            })

            _ <- resourceAccessValidator.verifyOwnership(resId, user.id)

            updatedAttachment = attachment.copy(
              description = newDescription.orElse(attachment.description),
              filename = newFilename.getOrElse(attachment.filename)
            )

            _ <- attachmentRepository.save(updatedAttachment)
            json <- attachmentWriter.asCurrentUser(updatedAttachment)

          } yield Ok(json)
      }
    }

  /**
    * Reemplaza un archivo existente por uno nuevo
    * PUT /api/resources/files/:attachmentId/replace
    */
  def replaceFile(attachmentId: String): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body
          val file = body.file("file").getOrElse(throw new BadRequestException("file is required"))

          for {
            oldAttachment <-
              attachmentRepository
                .findById(AttachmentId(attachmentId))
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Attachment not found")))

            resId <- Future.successful(oldAttachment.resourceId match {
              case Some(id: ResourceId) => id
              case None                 => throw new BadRequestException("This attachment is not associated with a resource")
            })

            _ <- resourceAccessValidator.verifyOwnership(resId, user.id)
            _ <- attachmentRepository.delete(oldAttachment.id)

            newAttachment <- saveAsAttachment(
              user = user,
              resourceId = Some(resId),
              id = None,
              file = file.ref.path.toFile,
              originalFilename = file.filename,
              description = body.dataParts.get("description").flatMap(_.headOption),
              contentType = file.contentType
            )

            json <- attachmentWriter.asCurrentUser(newAttachment)

          } yield Ok(json)
      }
    }

  private def uploadMultipleFiles(
      user: dev.pompilius.users.domain.User,
      body: MultipartFormData[Files.TemporaryFile],
      resourceId: ResourceId
  )(implicit request: play.api.mvc.Request[MultipartFormData[Files.TemporaryFile]]): Future[Seq[Attachment]] = {
    val files = body.files.filter(f => f.key == "files[]" || f.key == "file")

    Future.sequence(files.map { file =>
      saveAsAttachment(
        user = user,
        resourceId = Some(resourceId),
        id = None,
        file = file.ref.path.toFile,
        originalFilename = file.filename,
        description = body.dataParts.get(s"description_${file.filename}").flatMap(_.headOption),
        contentType = file.contentType
      )
    })
  }

  private def validateAccess(resourceId: ResourceId, userId: dev.pompilius.users.domain.UserId): Future[Unit] = {
    resourceRepository.findById(resourceId).flatMap {
      case Some(resource) =>
        resource.visibility match {
          case dev.pompilius.shared.domain.Visibility.PUBLIC =>
            Future.successful(())

          case _ =>
            resourceAccessValidator.verifyOwnership(resourceId, userId).recoverWith {
              case _ =>
                Future.failed(new ForbiddenException("You don't have access to this resource"))
            }
        }

      case None =>
        Future.failed(new ResourceNotFoundException(s"Resource not found"))
    }
  }
}
