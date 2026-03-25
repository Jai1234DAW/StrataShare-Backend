package dev.pompilius.studies.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.Request

object StudyAttachmentRequestParser {

  case class AddAttachmentRequest(
      attachmentId: String
  )

  def parseAddAttachment(request: Request[play.api.libs.json.JsValue]): AddAttachmentRequest = {
    try {
      val json = request.body

      val attachmentId = (json \ "attachmentId").as[String]

      AddAttachmentRequest(
        attachmentId = attachmentId
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid add attachment request")
    }
  }
}

