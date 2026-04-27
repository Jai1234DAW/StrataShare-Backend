package dev.pompilius.sample.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.sample.domain.request.UpdateSampleRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.infrastructure.StringUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object UpdateSampleRequestParser {

  implicit val jsonReads: Reads[UpdateSampleRequest] = (
    (__ \ Strings.name).readNullable[String] and
      (__ \ Strings.visibility).readNullable[String].map(_.map(Visibility.withNameInsensitive)) and
      (__ \ Strings.location).readNullable[String] and
      (__ \ Strings.observations).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.summary).readNullable[String].map(_.map(StringUtil.stripTags)) and

      (__ \ Strings.minerals).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.collectionMethods).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.isFresh).readNullable[Boolean] and
      (__ \ Strings.sampleType).readNullable[String] and
      (__ \ Strings.materialsUsed).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.sampleCategory).readNullable[String] and
      (__ \ Strings.geologicalProcesses).readNullable[String].map(_.map(StringUtil.stripTags))
  )(UpdateSampleRequest.apply _)

  def parse[A](request: Request[A]): UpdateSampleRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[UpdateSampleRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}

