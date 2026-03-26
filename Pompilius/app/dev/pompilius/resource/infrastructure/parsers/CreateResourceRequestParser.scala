package dev.pompilius.resource.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.resource.domain.request.CreateResourceRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateResourceRequestParser {

  implicit val jsonReads: Reads[CreateResourceRequest] = (
    (__ \ Strings.resourceType).read[String](ReadsUtil.nonEmpty) and
      (__ \ Strings.visibility).read[String](ReadsUtil.nonEmpty) and
      (__ \ Strings.localization).read[String](ReadsUtil.nonEmpty) and
      (__ \ Strings.observations).readNullable[String] and
      (__ \ Strings.summary).readNullable[String]
  )(CreateResourceRequest.apply _)

  def parse[A](request: Request[A]): CreateResourceRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateResourceRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}

