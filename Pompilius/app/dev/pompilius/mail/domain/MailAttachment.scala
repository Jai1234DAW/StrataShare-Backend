package dev.pompilius.mail.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils

import java.io.File
import java.net.URLConnection

sealed trait MailAttachmentDisposition extends EnumEntry

object MailAttachmentDisposition extends Enum[MailAttachmentDisposition] with PlayJsonEnum[MailAttachmentDisposition] {

  val values: IndexedSeq[MailAttachmentDisposition] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object INLINE extends MailAttachmentDisposition

  @SuppressWarnings(Array("ObjectNames"))
  case object ATTACHMENT extends MailAttachmentDisposition
}

case class MailAttachment(
    contentType: String,
    content: String,
    disposition: MailAttachmentDisposition,
    filename: Option[String],
    contentId: Option[String]
)

object MailAttachment {

  def apply(file: File, disposition: MailAttachmentDisposition, contentId: Option[String]): MailAttachment = {
    MailAttachment(
      contentType = Option(URLConnection.guessContentTypeFromName(file.getName)).getOrElse("text/plain"),
      content = Base64.encodeBase64String(FileUtils.readFileToByteArray(file)),
      disposition = disposition,
      filename = Some(file.getName),
      contentId = contentId
    )
  }
}
