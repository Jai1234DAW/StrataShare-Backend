package dev.pompilius.attachment.infrastructure

import dev.pompilius.Strings
import dev.pompilius.attachment.domain.{Attachment, AttachmentId, AttachmentRepository}
import dev.pompilius.shared.domain.exceptions.{BadRequestException, InternalServerException, NotFoundException}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import dev.pompilius.users.domain.User
import play.api.libs.Files
import play.api.libs.Files.TemporaryFileCreator
import play.api.libs.json.Json
import play.api.mvc.{MultipartFormData, Request, Result}

import java.awt.image.BufferedImage
import java.awt.{Color, Graphics2D, RenderingHints}
import java.io.{File, IOException}
import java.nio.file.Files.deleteIfExists
import javax.imageio.ImageIO
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

// Añade funcionalidades de subida y descarga de attachments a un controlador.
trait Attachments extends BaseController {

  private[this] var _temporaryFileCreator: TemporaryFileCreator = _

  @SuppressWarnings(Array("NullParameter"))
  def temporaryFileCreator: TemporaryFileCreator = {
    if (_temporaryFileCreator != null) _temporaryFileCreator
    else {
      throw new NoSuchElementException(
        "temporaryFileCreator not set! Call setTemporaryFileCreator or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def setTemporaryFileCreator(tfc: TemporaryFileCreator): Unit = {
    _temporaryFileCreator = tfc
  }

  private[this] var _attachmentRepository: AttachmentRepository = _

  @SuppressWarnings(Array("NullParameter"))
  def attachmentRepository: AttachmentRepository = {
    if (_attachmentRepository != null) _attachmentRepository
    else {
      throw new NoSuchElementException(
        "attachmentRepository not set! Call setAttachmentRepository or create the instance with dependency injection."
      )
    }
  }

  @Inject
  def attachmentRepository(ar: AttachmentRepository): Unit = {
    _attachmentRepository = ar
  }

  // Sube un archivo y lo guarda como un Attachment
  // El archivo se recibe en el campo "file" de un formulario multipart
  // Opcionalmente se puede enviar un campo "description" con una descripción del archivo
  // Devuelve el Attachment creado
  def upload(
      user: User,
      body: MultipartFormData[Files.TemporaryFile]
  )(implicit request: Request[MultipartFormData[Files.TemporaryFile]], ec: ExecutionContext): Future[Attachment] = {

    val file = body.file(Strings.file).getOrElse(throw new BadRequestException("file is required"))

    val description = body.dataParts.get("description").flatMap(_.headOption).filter(_.nonEmpty)
    val isPublic = body.dataParts.get("public").exists(_.headOption.contains("true"))
    val attachmentId = AttachmentId.gen(configuration.nodeId)

    // Guardamos el archivo en una ruta basada en la fecha actual y el ID del attachment
    val relativePath = s"${clock.now.toString("yyyyMM")}/${attachmentId.toString}"
    val outputFile = configuration.attachments.path.resolve(relativePath).toFile
    outputFile.getParentFile.mkdirs()

    val metadata = Some(
      Json
        .obj(
          List(
            toJsValueWrapper("originalFilename", file.filename),
            toJsValueWrapper("contentType", file.contentType),
            toJsValueWrapper("fileSize", file.fileSize),
            toJsValueWrapper("remoteAddress", remoteAddress),
            toJsValueWrapper("forwardedFor", request.headers.get("X-Forwarded-For")),
            toJsValueWrapper("userAgent", request.remoteAddress),
            toJsValueWrapper(Strings.userId, user.id.toString)
          ).flatten: _*
        )
        .toString()
    )

    // Guardar archivo en disco
    file.ref.copyTo(outputFile, replace = true)
    deleteIfExists(file.ref.path) // opcional: borrar temporal

    // Construcción del objeto Attachment
    val attachment = Attachment(
      id = attachmentId,
      node = configuration.nodeId,
      relativePath = relativePath,
      filename = file.filename.take(256),
      description = description.map(_.take(256)),
      contentType = file.contentType.getOrElse("application/octet-stream"),
      size = file.fileSize,
      createdAt = clock.now,
      isPublic = isPublic,
      metadata = metadata
    )

    // Para encadenar operaciones asíncronas con for
    for {
      _ <- attachmentRepository.save(attachment)
    } yield attachment
  }

  @SuppressWarnings(Array("NullParameter"))
  private def resizeImage(image: BufferedImage, maxWidth: Int, maxHeight: Int): BufferedImage = {
    val (width, height) = (image.getWidth, image.getHeight)

    val (newWidth, newHeight) =
      if (width <= maxWidth && height <= maxHeight) (width, height)
      else {
        val aspectRatio = width.toDouble / height.toDouble
        if (width > height) {
          (maxWidth, (maxWidth / aspectRatio).toInt)
        } else {
          ((maxHeight * aspectRatio).toInt, maxHeight)
        }
      }

    // Usar BufferedImage.TYPE_INT_RGB para eliminar transparencia
    val resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
    val graphics: Graphics2D = resizedImage.createGraphics()

    // Rellenar con fondo blanco para evitar problemas con imágenes con canal alfa
    graphics.setColor(Color.WHITE)
    graphics.fillRect(0, 0, newWidth, newHeight)

    // Aplicar interpolación para mejorar la calidad del escalado
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    graphics.drawImage(image, 0, 0, newWidth, newHeight, null)
    graphics.dispose()

    resizedImage
  }

  // Sube una imagen, la redimensiona si es necesario y la guarda como un Attachment JPEG
  // El archivo se recibe en el campo "file" de un formulario multipart
  // Opcionalmente se puede enviar un campo "description" con una descripción del archivo
  // Si encrypt es true, se genera una clave aleatoria para cifrar el archivo antes de guardarlo
  // maxWidth y maxHeight indican el tamaño máximo permitido para la imagen.
  // Si la imagen es más grande, se redimensiona manteniendo la relación de aspecto
  // Devuelve el Attachment creado
  def uploadImage(
      user: User,
      body: MultipartFormData[Files.TemporaryFile],
      maxWidth: Int,
      maxHeight: Int
  )(implicit request: Request[MultipartFormData[Files.TemporaryFile]], ec: ExecutionContext): Future[Attachment] = {
    val file = body.file(Strings.file).getOrElse(throw new BadRequestException("file field is required"))
    val description = body.dataParts.get("description").flatMap(_.headOption).filter(_.nonEmpty)

    val image =
      try {
        ImageIO.read(file.ref.path.toFile)
      } catch {
        case _: IOException =>
          throw new BadRequestException("Invalid image format")
      }

    val resizedImage =
      try {
        resizeImage(image, maxWidth, maxHeight)
      } catch {
        case _: OutOfMemoryError =>
          throw new BadRequestException("Invalid image")
      }

    val jpgFile = temporaryFileCreator.create(prefix = "image_", suffix = ".jpg")

    try {
      ImageIO.write(resizedImage, "jpg", jpgFile)
    } catch {
      case _: IOException =>
        throw new InternalServerException("Error processing image")
    }

    val metadata = Some(
      Json
        .obj(
          List(
            toJsValueWrapper("originalFilename", file.filename),
            toJsValueWrapper("contentType", file.contentType),
            toJsValueWrapper("originalFileSize", file.fileSize),
            toJsValueWrapper("originalWidth", image.getWidth),
            toJsValueWrapper("originalHeight", image.getHeight),
            toJsValueWrapper("fileSize", jpgFile.length()),
            toJsValueWrapper("width", resizedImage.getWidth),
            toJsValueWrapper("height", resizedImage.getHeight),
            toJsValueWrapper("remoteAddress", request.remoteAddress),
            toJsValueWrapper("forwardedFor", request.headers.get("X-Forwarded-For")),
            toJsValueWrapper(Strings.userId, user.id.toString)
          ).flatten: _*
        )
        .toString()
    )

    val attachmentId = AttachmentId.gen(configuration.nodeId)

    // Guardamos el archivo en una ruta basada en la fecha actual y el ID del attachment
    val relativePath = s"${clock.now.toString("yyyyMM")}/${attachmentId.toString}"
    val outputFile = configuration.attachments.path.resolve(relativePath).toFile
    outputFile.getParentFile.mkdirs()

    val isPublic = body.dataParts.get("public").exists(_.headOption.contains("true"))

    // Guardar archivo en disco
    jpgFile.copyTo(outputFile, replace = true)
    //Para borrar el temporal
    deleteIfExists(jpgFile.path)

    // Construcción del objeto Attachment
    val attachment = Attachment(
      id = attachmentId,
      node = configuration.nodeId,
      relativePath = relativePath,
      filename = file.filename.take(256),
      description = description.map(_.take(256)),
      contentType = file.contentType.getOrElse("application/octet-stream"),
      size = file.fileSize,
      createdAt = clock.now,
      isPublic = isPublic,
      metadata = metadata
    )

    // Para encadenar operaciones asíncronas con for
    for {
      _ <- attachmentRepository.save(attachment)
    } yield attachment
  }

  //Guardar como Attachment
  def saveAsAttachment[A](
      user: User,
      id: Option[AttachmentId],
      file: File,
      originalFilename: String,
      description: Option[String],
      contentType: Option[String]
  )(implicit request: Request[A], ec: ExecutionContext): Future[Attachment] = {
    val metadata = Some(
      Json
        .obj(
          List(
            toJsValueWrapper("originalFilename", originalFilename),
            toJsValueWrapper("contentType", contentType),
            toJsValueWrapper("fileSize", file.length()),
            toJsValueWrapper("remoteAddress", request.remoteAddress),
            toJsValueWrapper("forwardedFor", request.headers.get("X-Forwarded-For")),
            toJsValueWrapper("userAgent", request.headers.get("User-Agent")),
            toJsValueWrapper(Strings.userId, user.id.toString)
          ).flatten: _*
        )
        .toString()
    )

    // Si no se proporciona un ID, generamos uno nuevo
    val attachmentId = id.getOrElse(AttachmentId.gen(configuration.nodeId))

    // Guardamos el archivo en una ruta basada en la fecha actual y el ID del attachment
    val relativePath = s"${clock.now.toString("yyyyMM")}/${attachmentId.toString}"
    val outputFile = configuration.attachments.path.resolve(relativePath).toFile
    outputFile.getParentFile.mkdirs()

    java.nio.file.Files.copy(file.toPath, outputFile.toPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    // Borramos el archivo original
    deleteIfExists(file.toPath)

    val attachment = Attachment(
      id = attachmentId,
      node = configuration.nodeId,
      relativePath = relativePath,
      filename = originalFilename.take(256),
      description = description.map(_.take(256)),
      contentType = contentType.getOrElse("application/octet-stream"),
      size = file.length(),
      createdAt = clock.now,
      metadata = metadata
    )

    for {
      _ <- attachmentRepository.save(attachment)
    } yield attachment
  }

  // Descarga el attachment, asumiendo que estamos en el nodo correcto y que el usuario tiene permisos para verlo
  def doDownload[T](user: Option[User], attachment: Attachment)(implicit
      ec: ExecutionContext
  ): Future[Result] = {
    Future.successful {
      Ok.sendFile(
          content = configuration.attachments.path.resolve(attachment.relativePath).toFile,
          fileName = { _ => Some(attachment.filename) }
        )
        .as(attachment.contentType)
        .withHeaders(
          //CACHE_CONTROL -> "private, max-age=2592000, immutable" // 30 días
          CACHE_CONTROL -> "private, no-cache, must-revalidate"
        )
    }
  }

  // Se puede implementar una función de descarga el attachment, redirigiendo a otro nodo si es necesario
  def download[T](user: Option[User], attachmentId: AttachmentId)(implicit
      ec: ExecutionContext
  ): Future[Result] = {
    attachmentRepository
      .findById(attachmentId)
      .map(_.getOrElse(throw new NotFoundException(s"Attachment with id $attachmentId not found")))
      .flatMap {
        case attachment =>
          doDownload(user, attachment)
      }
  }
}
