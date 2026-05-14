package dev.pompilius.attachment.infrastructure.writers

import dev.pompilius.attachment.domain.Attachment
import com.google.inject.ImplementedBy
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.Strings
import dev.pompilius.users.domain.User
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AttachmentWriterImpl])
trait AttachmentWriter {
  def asCurrentUser(attachment: Attachment): Future[JsValue]
  def asAdmin(attachment: Attachment): Future[JsValue]
  def asList(attachments: List[Attachment], resourceId: String): Future[JsValue]
}

//Aqui puedo tener más cosas, por ejemplo el de admin puede tener la fingerprint y todo eso
@Singleton
class AttachmentWriterImpl @Inject() (implicit ex: ExecutionContext) extends AttachmentWriter {

  private def base(attachment: Attachment): Future[JsObject] =
    Future.successful {
      Json.obj(
        List(
          toJsValueWrapper(Strings.id, attachment.id.toString),
          toJsValueWrapper(Strings.filename, attachment.filename),
          toJsValueWrapper(Strings.description, attachment.description),
          toJsValueWrapper(Strings.contentType, attachment.contentType),
          toJsValueWrapper(Strings.size, attachment.size),
          toJsValueWrapper(Strings.createdAt, attachment.createdAt)
        ).flatten: _*
      )
    }

  override def asCurrentUser(attachment: Attachment): Future[JsValue] = {
    for {
      baseJson <- base(attachment)
    } yield {
      attachment.resourceId match {
        case Some(id) =>
          baseJson ++ Json.obj(Strings.resourceId -> id.toString)
        case None =>
          baseJson
      }
    }
  }

  def asAdmin(attachment: Attachment): Future[JsValue] = {
    for {
      baseJson <- base(attachment)
    } yield {
      baseJson ++ Json.obj(
        List(
          toJsValueWrapper(Strings.node, attachment.node),
          toJsValueWrapper(Strings.relativePath, attachment.relativePath),
          toJsValueWrapper(Strings.deleted, attachment.deleted),
          toJsValueWrapper(Strings.metadata, attachment.metadata)
        ).flatten: _*
      )
    }
  }

  override def asList(attachments: List[Attachment], resourceId: String): Future[JsValue] = {
    // Extrae el resourceId del primer attachment (todos deberían tener el mismo)

    for {
      attachmentsJson <- Future.sequence(attachments.map(asCurrentUser))
    } yield {
      Json.obj(
        "resourceId" -> resourceId,
        "uploadedFiles" -> attachmentsJson.length,
        "attachments" -> attachmentsJson
      )
    }
  }
}
