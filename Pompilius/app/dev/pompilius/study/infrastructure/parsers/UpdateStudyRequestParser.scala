package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.study.domain.request.UpdateStudyRequest
import dev.pompilius.study.domain.Area
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.StringUtil
import dev.pompilius.shared.infrastructure.JsUtils.JodaDateTimeReads
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object UpdateStudyRequestParser {

  implicit val jsonReads: Reads[UpdateStudyRequest] = (
    (__ \ Strings.visibility).readNullable[String].map(_.map(Visibility.withNameInsensitive)) and
      (__ \ Strings.localization).readNullable[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.name).readNullable[String] and
      (__ \ Strings.startDate).readNullable[DateTime](JodaDateTimeReads) and
      (__ \ Strings.endDate).readNullable[DateTime](JodaDateTimeReads) and
      (__ \ Strings.description).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.coordinates).readNullable[String] and
      (__ \ Strings.area).readNullable[String].map(_.map(Area.withNameInsensitive)) and
      (__ \ Strings.methods).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.authors).readNullable[String] and
      (__ \ Strings.section).readNullable[Boolean] and
      (__ \ Strings.antecedents).readNullable[Boolean] and
      (__ \ Strings.nameSection).readNullable[String]
  )(UpdateStudyRequest.apply _)

  def parse[A](request: Request[A]): UpdateStudyRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[UpdateStudyRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
