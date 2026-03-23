package dev.pompilius.mail.domain

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.FileUtils

import java.io.File
import java.net.URLConnection

case class MailAttachment(
    contentType: String,
    content: String,
    disposition: AttachmentDisposition,
    filename: Option[String],
    contentId: Option[String]
)

object MailAttachment {

  def apply(file: File, disposition: AttachmentDisposition, contentId: Option[String]): MailAttachment = {
    MailAttachment(
      contentType = Option(URLConnection.guessContentTypeFromName(file.getName)).getOrElse("text/plain"),
      content = Base64.encodeBase64String(FileUtils.readFileToByteArray(file)),
      disposition = disposition,
      filename = Some(file.getName),
      contentId = contentId
    )
  }

}
