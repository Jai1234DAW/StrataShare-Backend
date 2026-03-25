package dev.pompilius.studies.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.studies.domain.Study
import org.joda.time.DateTime
import play.api.mvc.Request

object CreateStudyRequestParser {

  def parse(request: Request[play.api.libs.json.JsValue]): Study = {
    try {
      val json = request.body

      val name = (json \ "name").as[String]
      val visibility = (json \ "visibility").as[String]
      val localization = (json \ "localization").as[String]
      val startDate = (json \ "startDate").as[Long]
      val endDate = (json \ "endDate").asOpt[Long]
      val description = (json \ "description").asOpt[String]
      val coordinates = (json \ "coordinates").asOpt[String]
      val observations = (json \ "observations").asOpt[String]
      val summary = (json \ "summary").asOpt[String]

      val now = DateTime.now()

      Study(
        id = new dev.pompilius.studies.domain.StudyId(0), // Se generará en la base de datos
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
        updated = now
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid create study request")
    }
  }
}

