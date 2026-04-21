package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.study.domain.request.CreateStudyRequest
import dev.pompilius.study.domain.Area
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.StringUtil
import dev.pompilius.shared.infrastructure.JsUtils.JodaDateTimeReads
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateStudyRequestParser {

  implicit val jsonReads: Reads[CreateStudyRequest] = (
    (__ \ Strings.name).read[String] and
      (__ \ Strings.visibility).read[String].map(Visibility.withNameInsensitive) and
      (__ \ Strings.localization).read[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.startDate).read[DateTime](JodaDateTimeReads) and
      (__ \ Strings.endDate).readNullable[DateTime](JodaDateTimeReads) and
      (__ \ Strings.description).read[String].map(StringUtil.stripTags) and
      (__ \ Strings.coordinates).read[String] and
      (__ \ Strings.area).read[String].map(Area.withNameInsensitive) and
      (__ \ Strings.methods).read[String] and
      (__ \ Strings.authors).read[String] and
      (__ \ Strings.section).read[Boolean] and
      (__ \ Strings.antecedents).read[Boolean] and
      (__ \ Strings.nameSection).readNullable[String]
  )(CreateStudyRequest.apply _)

  def parse[A](request: Request[A]): CreateStudyRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        val createStudyRequest = json.as[CreateStudyRequest]

        // Validar que startDate no sea más reciente que hoy
        val today = DateTime.now.withTimeAtStartOfDay()
        if (createStudyRequest.startDate.isAfter(today)) {
          throw new BadRequestException("Start date cannot be in the future (must be today or earlier)")
        }

        // Validar que endDate (si existe) no sea menor que startDate
        if (
          createStudyRequest.endDate.isDefined &&
          createStudyRequest.endDate.get.isBefore(createStudyRequest.startDate)
        ) {
          throw new BadRequestException("End date cannot be before start date")
        }

        createStudyRequest
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}

