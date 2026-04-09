package dev.pompilius.barter.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.barter.domain.request.CreateBarterRequest
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateBarterRequestParser {

  implicit val jsonReads: Reads[CreateBarterRequest] = (
    (__ \ Strings.resourceId).read[String].map(ResourceId(_)) and
      (__ \ Strings.offeredResourceId).read[String].map(ResourceId(_))
  )(CreateBarterRequest.apply _)

  def parse[A](request: Request[A]): CreateBarterRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateBarterRequest]

      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
