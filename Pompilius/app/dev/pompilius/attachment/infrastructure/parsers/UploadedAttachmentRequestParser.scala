package dev.pompilius.attachment.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.attachment.domain.request.UploadedAttachmentRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.{AnyContentAsJson, Request}

object UploadedAttachmentRequestParser {

  def parse[A](request: Request[A]): UploadedAttachmentRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        UploadedAttachmentRequest(id = AttachmentId((json \ Strings.id).as[String]))
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
