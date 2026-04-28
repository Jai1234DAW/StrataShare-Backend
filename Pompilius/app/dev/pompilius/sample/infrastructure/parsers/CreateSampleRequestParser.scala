package dev.pompilius.sample.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.sample.domain.request.CreateSampleRequest
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.JsUtils.JodaDateTimeReads
import dev.pompilius.shared.infrastructure.StringUtil
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.Files
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, MultipartFormData, Request}

object CreateSampleRequestParser {

  implicit val jsonReads: Reads[CreateSampleRequest] = (
    (__ \ Strings.name).read[String] and
      (__ \ Strings.visibility).read[String].map(Visibility.withNameInsensitive) and
      (__ \ Strings.location).read[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.price).readNullable[BigDecimal] and
      (__ \ Strings.isBarter).read[Boolean] and
      (__ \ Strings.collectedDate)
        .read[DateTime]
        .map(_.withZone(DateTimeZone.UTC).withTimeAtStartOfDay())
        .filter(JsonValidationError("error.date.future")) { collectedDate =>
          val today = DateTime.now(DateTimeZone.UTC).withTimeAtStartOfDay()
          !collectedDate.isAfter(today)
        } and
      (__ \ Strings.minerals).readNullable[String] and
      (__ \ Strings.collectionMethods).readNullable[String] and
      (__ \ Strings.isFresh).read[Boolean] and
      (__ \ Strings.sampleType).readNullable[String] and
      (__ \ Strings.materialsUsed).readNullable[String] and
      (__ \ Strings.sampleCategory).readNullable[String] and
      (__ \ Strings.geologicalProcesses).readNullable[String]
  )(CreateSampleRequest.apply _).filter(JsonValidationError("Invalid price/barter/visibility combination")) { req =>
    req.visibility match {
      case Visibility.PUBLIC =>
        req.price.isEmpty && !req.isBarter

      case Visibility.PRIVATE =>
        !(req.price.isDefined && req.isBarter)

      case _ =>
        false
    }
  }

  def parse[A](request: Request[A]): CreateSampleRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateSampleRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

  def parseMultipart(body: MultipartFormData[Files.TemporaryFile]): CreateSampleRequest = {
    val json = body.dataParts
      .get("data")
      .flatMap(_.headOption)
      .map(Json.parse)
      .getOrElse(throw new BadRequestException("Missing data field"))

    parseJson(json)
  }

  private def parseJson(json: JsValue): CreateSampleRequest = {
    json.validate[CreateSampleRequest] match {
      case JsSuccess(value, _) =>
        value

      case JsError(errors) =>
        val message = errors
          .map {
            case (path, validationErrors) =>
              val msgs = validationErrors.map(_.message).mkString(", ")
              s"${path.toJsonString}: $msgs"
          }
          .mkString("; ")

        throw new BadRequestException(message)
    }
  }
}
