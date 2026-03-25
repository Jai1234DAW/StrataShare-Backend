package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.study.domain.{Study, Area, Visibility, StudyId}
import org.joda.time.DateTime
import play.api.mvc.Request

object CreateStudyRequestParser {

  def parse(request: Request[play.api.libs.json.JsValue]): Study = {
    try {
      val json = request.body

      val name = (json \ "name").as[String]
      val visibilityStr = (json \ "visibility").as[String]
      val visibility = Visibility.withName(visibilityStr)
      val localization = (json \ "localization").as[String]
      val startDate = (json \ "startDate").as[Long]
      val endDate = (json \ "endDate").asOpt[Long]
      val description = (json \ "description").as[String]
      val coordinates = (json \ "coordinates").as[String]
      val observations = (json \ "observations").asOpt[String]
      val summary = (json \ "summary").asOpt[String]
      val areaStr = (json \ "area").as[String]
      val area = Area.withName(areaStr)
      val methods = (json \ "methods").as[String]
      val authors = (json \ "authors").as[String]
      val antecedent = (json \ "antecedent").as[Boolean]
      val section = (json \ "section").as[Boolean]
      val nameSection = (json \ "nameSection").asOpt[String]

      val now = DateTime.now()

      Study(
        id = StudyId.gen(1),
        name = name,
        visibility = visibility,
        localization = localization,
        startDate = new DateTime(startDate),
        endDate = endDate.map(new DateTime(_)),
        description = description,
        coordinates = coordinates,
        observations = observations,
        summary = summary,
        created = now,
        updated = now,
        area = area,
        methods = methods,
        authors = authors,
        antecedent = antecedent,
        section = section,
        nameSection = nameSection
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid create study request")
    }
  }
}

