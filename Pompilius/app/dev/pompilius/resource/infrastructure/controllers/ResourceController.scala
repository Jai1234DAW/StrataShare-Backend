package dev.pompilius.resource.infrastructure.controllers

import dev.pompilius.attachment.domain.exceptions.AttachmentNotFoundException
import dev.pompilius.attachment.domain.{Attachment, AttachmentId, AttachmentRepository}
import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.attachment.infrastructure.writers.AttachmentWriter
import dev.pompilius.resource.domain.exceptions.ResourceNotFoundException
import dev.pompilius.resource.domain.{ResourceAccessLevel, ResourceId, ResourceRepository}
import dev.pompilius.resource.infrastructure.ResourceAccessValidator
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.domain.exceptions.{BadRequestException, ForbiddenException}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.Role
import play.api.libs.Files
import play.api.mvc.{Action, AnyContent, MultipartFormData}

import javax.imageio.ImageIO
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random.nextInt

@Singleton
class ResourceController @Inject() (
    resourceRepository: ResourceRepository,
    attachmentRepository: AttachmentRepository,
    resourceAccessValidator: ResourceAccessValidator,
    attachmentWriter: AttachmentWriter
)(implicit val ec: ExecutionContext)
    extends BaseController
    with Attachments {

  def uploadFiles(resourceId: String): Action[MultipartFormData[Files.TemporaryFile]] =
    Action.async(parse.multipartFormData) { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val body = request.body
          val rid = ResourceId(resourceId)

          resourceAccessValidator.verifyOwnership(rid, user.id)

          if (body.files.isEmpty) {
            Future.failed(new BadRequestException("No files uploaded"))
          } else if (body.files.exists(checkIsImage)) {
            Future.failed(new BadRequestException("Images are not allowed in this endpoint"))
          } else {
            for {
              _ <-
                resourceRepository
                  .findById(rid)
                  .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

              attachments <- uploadMultipleFiles(user, body, rid)
              response <- attachmentWriter.asList(attachments.toList)

            } yield Ok(response)
          }
      }
    }

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

  def getPreviewImage(resourceId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ResourceId(resourceId)

          for {
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            previewImage <-
              attachmentRepository
                .findPreviewImageByResourceId(rid)
                .map(
                  _.getOrElse(throw new ResourceNotFoundException(s"No preview image found for resource $rid"))
                )
            result <- download(Some(user), previewImage.id)
          } yield result
      }
    }

  def setPreviewImage(resourceId: String, attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ResourceId(resourceId)
          val aid = AttachmentId(attachmentId)

          for {
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            attachment <-
              attachmentRepository
                .findById(aid)
                .map(_.getOrElse(throw new AttachmentNotFoundException(s"Attachment $aid not found")))

            // Validar que el attachment pertenece al recurso indicado
            _ <- attachment.resourceId match {
              case Some(attachmentResourceId) if attachmentResourceId == rid =>
                Future.successful(())
              case Some(other) =>
                Future.failed(new AttachmentNotFoundException(s"Attachment $aid does not belong to resource $rid"))
              case None =>
                Future.failed(new AttachmentNotFoundException("This attachment is not associated with a resource"))
            }

            _ <- resourceAccessValidator.verifyOwnership(rid, user.id)

            _<-attachmentRepository.findPreviewImageByResourceId(rid).flatMap {
              case Some(existingPreview) if existingPreview.id != aid =>

                // Si ya hay una imagen de preview diferente, quitarle el flag
                val updatedExisting = existingPreview.copy(previewImage = false)
                attachmentRepository.save(updatedExisting)
              case _ =>
                Future.successful(())
            }

            _ <- attachmentRepository.setPreviewImageByResourceId(rid, aid)

          } yield Ok
      }
    }

  def downloadFile(resourceId: String, attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val aid = AttachmentId(attachmentId)
          val rid = ResourceId(resourceId)

          for {
            resource <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            attachment <-
              attachmentRepository
                .findById(aid)
                .map(_.getOrElse(throw new AttachmentNotFoundException(s"Attachment $aid not found")))

            // Validar que el attachment pertenece al recurso indicado
            _ <- attachment.resourceId match {
              case Some(attachmentResourceId) if attachmentResourceId == rid =>
                Future.successful(())
              case Some(other) =>
                Future.failed(new BadRequestException(s"Attachment $aid does not belong to resource $rid"))
              case None =>
                Future.failed(new BadRequestException("This attachment is not associated with a resource"))
            }

            // Verificar que el usuario tiene acceso completo (no solo preview)
            accessLevel <- resourceAccessValidator.getAccessLevel(rid, user.id)
            _ <- accessLevel match {
              case ResourceAccessLevel.FULL_ACCESS =>
                // Puede descargar attachments
                Future.successful(())
              case _ =>
                // Solo tiene preview, no puede descargar attachments
                Future.failed(
                  new ForbiddenException("You need to purchase this resource to download attachments")
                )
            }

            result <- download(Some(user), aid)

          } yield result
      }
    }

  def deleteFile(resourceId: String, attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val aid = AttachmentId(attachmentId)
          val rid = ResourceId(resourceId)
          for {
            resource <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            attachment <-
              attachmentRepository
                .findById(aid)
                .map(_.getOrElse(throw new AttachmentNotFoundException(s"Attachment $aid not found")))

            // Validar que el attachment pertenece al recurso indicado
            _ <- attachment.resourceId match {
              case Some(attachmentResourceId) if attachmentResourceId == rid =>
                Future.successful(())
              case Some(other) =>
                Future.failed(new BadRequestException(s"Attachment $aid does not belong to resource $rid"))
              case None =>
                Future.failed(new BadRequestException("This attachment is not associated with a resource"))
            }

            _ <- resourceAccessValidator.verifyOwnership(rid, user.id)
            _ <- attachmentRepository.delete(attachment.id)

          } yield {
            Ok
          }
      }
    }

  def listFiles(resourceId: String, pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val rid = ResourceId(resourceId)

          for {
            // Verificar que el recurso existe
            _ <-
              resourceRepository
                .findById(rid)
                .map(_.getOrElse(throw new ResourceNotFoundException(s"Resource $rid not found")))

            // Verificar acceso y obtener attachments solo si tiene permiso
            accessLevel <- resourceAccessValidator.getAccessLevel(rid, user.id)
            allAttachments <- accessLevel match {
              case ResourceAccessLevel.FULL_ACCESS =>
                // Tiene acceso completo → Buscar attachments
                attachmentRepository.findByResourceId(rid, pag.oneMore)
              case _ =>
                // Solo tiene preview → No puede ver attachments
                Future.failed(
                  new ForbiddenException("You need to purchase this resource to view attachments")
                )
            }

            // Formatear respuesta
            response <- attachmentWriter.asList(allAttachments)

          } yield {
            Ok(response)
          }
      }
    }

  //
  //  /**
  //    * Obtiene información de un archivo específico por ID
  //    * GET /api/resources/files/:attachmentId/info
  //    */
  //  def getFile(attachmentId: String): Action[AnyContent] =
  //    Action.async { implicit request =>
  //      val aid=AttachmentId(attachmentId)
  //      withAuthenticatedUser {
  //        case (_, user, _) =>
  //          attachmentRepository.findById(aid).flatMap {
  //            case Some(attachment) =>
  //              attachment.resourceId match {
  //                case Some(resId) =>
  //                  resourceAccessValidator.validateAccess(resId, user.id).flatMap { _ =>
  //                    attachmentWriter.asCurrentUser(attachment)
  //                  }
  //                case None =>
  //                  Future.failed(new BadRequestException("This attachment is not associated with a resource"))
  //              }
  //            case None =>
  //              Future.failed(new ResourceNotFoundException(s"Attachment $attachmentId not found"))
  //          }
  //      }
  //    }
  //
  def updateFileMetadata(resourceId: String, attachmentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val rid = ResourceId(resourceId)
          val aid = AttachmentId(attachmentId)
          val body = request.body.asJson.getOrElse(throw new BadRequestException("JSON body expected"))

          val newDescription = (body \ "description").asOpt[String]
          val newFilename = (body \ "filename").asOpt[String]

          for {
            attachment <-
              attachmentRepository
                .findById(aid)
                .map(_.getOrElse(throw new AttachmentNotFoundException(s"Attachment not found")))

            // Validar que el attachment pertenece al recurso indicado
            _ <- attachment.resourceId match {
              case Some(attachmentResourceId) if attachmentResourceId == rid =>
                Future.successful(())
              case Some(other) =>
                Future.failed(new BadRequestException(s"Attachment $aid does not belong to resource $rid"))
              case None =>
                Future.failed(new BadRequestException("This attachment is not associated with a resource"))
            }

            _ <- resourceAccessValidator.verifyOwnership(rid, user.id)

            updatedAttachment = attachment.copy(
              description = newDescription.orElse(attachment.description),
              filename = newFilename.getOrElse(attachment.filename)
            )

            _ <- attachmentRepository.save(updatedAttachment)
            json <- attachmentWriter.asCurrentUser(updatedAttachment)

          } yield Ok(json)
      }
    }

  //  def replaceFile(attachmentId: String): Action[MultipartFormData[Files.TemporaryFile]] =
  //    Action.async(parse.multipartFormData) { implicit request =>
  //      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
  //        case (_, user, _, _) =>
  //          val body = request.body
  //          val file = body.file("file").getOrElse(throw new BadRequestException("file is required"))
  //
  //          for {
  //            oldAttachment <-
  //              attachmentRepository
  //                .findById(AttachmentId(attachmentId))
  //                .map(_.getOrElse(throw new ResourceNotFoundException(s"Attachment not found")))
  //
  //            resId <- Future.successful(oldAttachment.resourceId match {
  //              case Some(id: ResourceId) => id
  //              case None                 => throw new BadRequestException("This attachment is not associated with a resource")
  //            })
  //
  //            _ <- resourceAccessValidator.verifyOwnership(resId, user.id)
  //            _ <- attachmentRepository.delete(oldAttachment.id)
  //
  //            newAttachment <- saveAsAttachment(
  //              user = user,
  //              resourceId = Some(resId),
  //              id = None,
  //              file = file.ref.path.toFile,
  //              originalFilename = file.filename,
  //              description = body.dataParts.get("description").flatMap(_.headOption),
  //              contentType = file.contentType
  //            )
  //
  //            json <- attachmentWriter.asCurrentUser(newAttachment)
  //
  //          } yield Ok(json)
  //      }
  //    }

  private def uploadMultipleFiles(
      user: dev.pompilius.users.domain.User,
      body: MultipartFormData[Files.TemporaryFile],
      resourceId: ResourceId
  )(implicit request: play.api.mvc.Request[MultipartFormData[Files.TemporaryFile]]): Future[Seq[Attachment]] = {
    val files = body.files

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

  private def checkIsImage(file: MultipartFormData.FilePart[Files.TemporaryFile]): Boolean = {
    val byMime = file.contentType.exists(_.startsWith("image/"))
    val byExtension = file.filename.toLowerCase.matches(".*\\.(jpg|jpeg|png|gif|bmp)$")
    val byContent =
      try {
        ImageIO.read(file.ref.path.toFile) != null
      } catch {
        case _: Exception => false
      }
    byMime || byExtension || byContent
  }
}
