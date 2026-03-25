package dev.pompilius.sample.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.Request

object UpdateSampleRequestParser {

  case class UpdateSampleRequest(
      name: Option[String] = None,
      description: Option[String] = None,
      minerals: Option[String] = None,
      collectionMethods: Option[String] = None,
      isFresh: Option[Boolean] = None,
      sampleType: Option[String] = None,
      materialsUsed: Option[String] = None,
      rockType: Option[String] = None,
      geologicalProcesses: Option[String] = None
  )

  def parse(request: Request[play.api.libs.json.JsValue]): UpdateSampleRequest = {
    try {
      val json = request.body

      UpdateSampleRequest(
        name = (json \ "name").asOpt[String],
        description = (json \ "description").asOpt[String],
        minerals = (json \ "minerals").asOpt[String],
        collectionMethods = (json \ "collectionMethods").asOpt[String],
        isFresh = (json \ "isFresh").asOpt[Boolean],
        sampleType = (json \ "sampleType").asOpt[String],
        materialsUsed = (json \ "materialsUsed").asOpt[String],
        rockType = (json \ "rockType").asOpt[String],
        geologicalProcesses = (json \ "geologicalProcesses").asOpt[String]
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid update sample request")
    }
  }
}

