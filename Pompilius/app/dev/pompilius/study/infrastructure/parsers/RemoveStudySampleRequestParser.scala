package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.Request

object RemoveStudySampleRequestParser {

  case class RemoveStudySampleRequest(
      sampleId: String
  )

  def parse(request: Request[play.api.libs.json.JsValue]): RemoveStudySampleRequest = {
    try {
      val json = request.body

      val sampleId = (json \ "sampleId").as[String]

      RemoveStudySampleRequest(
        sampleId = sampleId
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid remove sample from study request")
    }
  }
}

