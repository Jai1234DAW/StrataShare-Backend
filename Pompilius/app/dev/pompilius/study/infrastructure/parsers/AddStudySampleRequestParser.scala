package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.Request

object AddStudySampleRequestParser {

  case class AddStudySampleRequest(
      sampleId: String
  )

  def parse(request: Request[play.api.libs.json.JsValue]): AddStudySampleRequest = {
    try {
      val json = request.body

      val sampleId = (json \ "sampleId").as[String]

      AddStudySampleRequest(
        sampleId = sampleId
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid add sample to study request")
    }
  }
}

