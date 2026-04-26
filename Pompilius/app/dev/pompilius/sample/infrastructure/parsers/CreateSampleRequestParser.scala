package dev.pompilius.sample.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.sample.domain.request.CreateSampleRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.StringUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateSampleRequestParser {

  implicit val jsonReads: Reads[CreateSampleRequest] = (
    (__ \ Strings.name).read[String] and
    (__ \ Strings.visibility).read[String].map(Visibility.withNameInsensitive) and
      (__ \ Strings.location).read[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.price).readNullable[BigDecimal] and
      (__ \ Strings.isBarter).read[Boolean] and


      (__ \ Strings.minerals).readNullable[String] and
      (__ \ Strings.collectionMethods).readNullable[String] and
      (__ \ Strings.isFresh).read[Boolean] and
      (__ \ Strings.sampleType).readNullable[String] and
      (__ \ Strings.materialsUsed).readNullable[String] and
      (__ \ Strings.rockType).readNullable[String] and
      (__ \ Strings.geologicalProcesses).readNullable[String]
  )(CreateSampleRequest.apply _)

  def parse[A](request: Request[A]): CreateSampleRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateSampleRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}

