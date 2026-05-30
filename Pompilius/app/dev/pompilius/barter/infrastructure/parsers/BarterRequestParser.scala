package dev.pompilius.barter.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.barter.domain.request.BarterRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}
import play.api.Logger

object BarterRequestParser {

  private val logger = Logger(this.getClass)

  implicit val jsonReads: Reads[BarterRequest] = (
      (__ \ Strings.barterId).read[String] and
        (__ \ Strings.transactionId).read[String]
  )(BarterRequest.apply _)

  def parse[A](request: Request[A]): BarterRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        logger.info(s"[BARTER PARSER] Raw JSON request: ${Json.stringify(json)}")
        val result = json.as[BarterRequest]
        logger.info(s"[BARTER PARSER] Parsed - barterId: ${result.barterId}, transactionId: ${result.transactionId}")
        result
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
