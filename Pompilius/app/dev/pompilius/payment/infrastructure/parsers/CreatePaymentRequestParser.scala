package dev.pompilius.payment.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain.request.CreatePaymentRequest
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}

object CreatePaymentRequestParser {
  implicit val jsonReads: Reads[CreatePaymentRequest] = (
    (__ \ Strings.resourceId).read[String].map(id => ResourceId(id)) and
      (__ \ Strings.gateway).read[String].map(Gateway.withNameInsensitive) and
      (__ \ Strings.buyerReference).readNullable[String](ReadsUtil.buyerReference) and
      (__ \ Strings.instrument).readNullable[String](ReadsUtil.paymentInstrument) and
      (__ \ Strings.returnUrlParams).readNullable[Map[String, String]] and
      (__ \ Strings.extraInfo).readNullable[String](ReadsUtil.extraInfo).map(_.map(_.trim).filter(_.nonEmpty))
  )(CreatePaymentRequest.apply _)

  def parse[A](request: Request[A]): CreatePaymentRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreatePaymentRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}
