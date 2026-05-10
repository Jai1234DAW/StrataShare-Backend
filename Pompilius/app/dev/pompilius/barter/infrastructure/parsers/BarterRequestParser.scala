package dev.pompilius.barter.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.barter.domain.request.BarterRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object BarterRequestParser {

  implicit val jsonReads: Reads[BarterRequest] = (
      (__ \ Strings.barterId).read[String] and
        (__ \ Strings.transactionId).read[String]
  )(BarterRequest.apply _)

  def parse[A](request: Request[A]): BarterRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[BarterRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
