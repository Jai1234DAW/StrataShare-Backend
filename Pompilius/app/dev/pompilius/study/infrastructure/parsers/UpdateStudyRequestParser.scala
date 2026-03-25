package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.study.domain.{Area, Visibility}
import org.joda.time.DateTime
import play.api.mvc.Request

object UpdateStudyRequestParser {

  case class UpdateStudyRequest(
      name: Option[String] = None,
      visibility: Option[Visibility] = None,
      localization: Option[String] = None,
      startDate: Option[Long] = None,
      endDate: Option[Long] = None,
      description: Option[String] = None,
      coordinates: Option[String] = None,
      observations: Option[String] = None,
      summary: Option[String] = None,
      area: Option[Area] = None,
      methods: Option[String] = None,
      authors: Option[String] = None,
      antecedent: Option[Boolean] = None,
      section: Option[Boolean] = None,
      nameSection: Option[String] = None
  )

  def parse(request: Request[play.api.libs.json.JsValue]): UpdateStudyRequest = {
    try {
      val json = request.body

      UpdateStudyRequest(
        name = (json \ "name").asOpt[String],
        visibility = (json \ "visibility").asOpt[String].flatMap(Visibility.withNameOption),
        localization = (json \ "localization").asOpt[String],
        startDate = (json \ "startDate").asOpt[Long],
        endDate = (json \ "endDate").asOpt[Long],
        description = (json \ "description").asOpt[String],
        coordinates = (json \ "coordinates").asOpt[String],
        observations = (json \ "observations").asOpt[String],
        summary = (json \ "summary").asOpt[String],
        area = (json \ "area").asOpt[String].flatMap(Area.withNameOption),
        methods = (json \ "methods").asOpt[String],
        authors = (json \ "authors").asOpt[String],
        antecedent = (json \ "antecedent").asOpt[Boolean],
        section = (json \ "section").asOpt[Boolean],
        nameSection = (json \ "nameSection").asOpt[String]
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid update study request")
    }
  }
}

