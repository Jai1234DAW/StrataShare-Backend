package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.study.domain.request.CreateStudyRequest
import dev.pompilius.study.domain.Area
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.StringUtil
import dev.pompilius.shared.infrastructure.JsUtils.JodaDateTimeReads
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateStudyRequestParser {

  implicit val jsonReads: Reads[CreateStudyRequest] = (
    (__ \ Strings.name).read[String] and
      (__ \ Strings.visibility).read[String].map(Visibility.withNameInsensitive) and
      (__ \ Strings.location).read[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.price).readNullable[BigDecimal] and
      (__ \ Strings.isBarter).read[Boolean] and
      (__ \ Strings.startDate)
        .read[DateTime]
        .map(_.withZone(DateTimeZone.UTC).withTimeAtStartOfDay())
        .filter(JsonValidationError("error.startDate.future")) { startDate =>
          val today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay()
          !startDate.isAfter(today)
        } and
      (__ \ Strings.endDate)
        .readNullable[DateTime]
        .map(_.map(_.withZone(DateTimeZone.UTC).withTimeAtStartOfDay())) and
      (__ \ Strings.description).read[String].map(StringUtil.stripTags) and
      (__ \ Strings.coordinates).read[String] and
      (__ \ Strings.area).read[String].map(Area.withNameInsensitive) and
      (__ \ Strings.methods).read[String] and
      (__ \ Strings.authors).read[String] and
      (__ \ Strings.section).read[Boolean] and
      (__ \ Strings.antecedents).read[Boolean] and
      (__ \ Strings.nameSection).readNullable[String]
  )(CreateStudyRequest.apply _).filter(JsonValidationError("Invalid price/barter/visibility combination")) { req =>
    req.visibility match {
      case Visibility.PUBLIC =>
        req.price.isEmpty && !req.isBarter

      case Visibility.PRIVATE =>
        !(req.price.isDefined && req.isBarter)

      case _ =>
        false
    }
  }.filter(JsonValidationError("error.endDate.before.startDate")) { req =>
    req.endDate.forall(endDate => !endDate.isBefore(req.startDate))
  }

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
