package dev.pompilius.payment.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain.{PaymentId, PaymentIntent, PaymentIntentStatus}
import dev.pompilius.shared.infrastructure.JsUtils.JodaDateTimeFormat
import dev.pompilius.transaction.domain.TransactionId
import org.joda.time.DateTime
import play.api.libs.json._

object CreatePaymentIntentRequestParser {

  implicit val jsonReads: Reads[PaymentIntent] = Reads[PaymentIntent] { js: JsValue =>
    try {
      JsSuccess(
        PaymentIntent(
          paymentId = PaymentId((js \ Strings.paymentId).as[String]),
          transactionId = TransactionId((js \ Strings.transactionId).as[String]),
          //buyerId = UserId((js \ Strings.userId).as[String]),
          //sellerId = UserId((js \ Strings.sellerId).as[String]),
          gateway = (js \ Strings.gateway).as[Gateway],
          gatewayIntentId = (js \ Strings.gatewayIntentId).as[String],
          price = (js \ Strings.price).as[BigDecimal],
          surcharge = (js \ Strings.surcharge).as[BigDecimal],
          amount = (js \ Strings.amount).as[BigDecimal],
          status = (js \ Strings.status).as[PaymentIntentStatus],
          discount = (js \ Strings.discount).asOpt[BigDecimal],
          url = (js \ Strings.url).asOpt[String],
          created = (js \ Strings.created).as[DateTime],
          buyerReference = (js \ Strings.buyerReference).asOpt[String],
          instrument = (js \ Strings.instrument).asOpt[String],
          fingerprint = (js \ Strings.fingerprint).asOpt[String],
          returnUrlParams = (js \ Strings.returnUrlParams).asOpt[Map[String, String]],
          //smartLinkId = (js \ Strings.smartLinkId).asOpt[String].map(SmartLinkId(_)),
          metadata = (js \ Strings.metadata).asOpt[String],
          extraInfo = (js \ Strings.extraInfo).asOpt[String]
        )
      )
    } catch {
      case e: JsResultException => JsError(e.errors)
    }
  }

  def parse(s: String): PaymentIntent = {
    Json.parse(s).as[PaymentIntent]
  }

  def parse(js: JsValue): PaymentIntent = {
    js.as[PaymentIntent]
  }
}
