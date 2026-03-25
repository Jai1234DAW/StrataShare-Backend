package dev.pompilius.studies.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import org.joda.time.DateTime
import play.api.mvc.Request

object UpdateStudyRequestParser {

  case class UpdateStudyRequest(
      name: Option[String] = None,
      visibility: Option[String] = None,
      localization: Option[String] = None,
      startDate: Option[Long] = None,
      endDate: Option[Long] = None,
      description: Option[String] = None,
      coordinates: Option[String] = None,
      observations: Option[String] = None,
      summary: Option[String] = None
  )

  def parse(request: Request[play.api.libs.json.JsValue]): UpdateStudyRequest = {
    try {
      val json = request.body

      UpdateStudyRequest(
        name = (json \ "name").asOpt[String],
        visibility = (json \ "visibility").asOpt[String],
        localization = (json \ "localization").asOpt[String],
        startDate = (json \ "startDate").asOpt[Long],
        endDate = (json \ "endDate").asOpt[Long],
        description = (json \ "description").asOpt[String],
        coordinates = (json \ "coordinates").asOpt[String],
        observations = (json \ "observations").asOpt[String],
        summary = (json \ "summary").asOpt[String]
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid update study request")
    }
  }
}

