package dev.pompilius.payment.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.payment.domain.PaymentIntent
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.transaction.domain.Transaction
import jakarta.inject.Singleton
import play.api.Configuration
import play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.Future

@ImplementedBy(classOf[PaymentIntentWriterImpl])
trait PaymentIntentWriter {
  def toJson(intent: PaymentIntent, transaction: Transaction): Future[JsValue]
}

@Singleton
class PaymentIntentWriterImpl @Inject() (configuration: Configuration) extends PaymentIntentWriter {

  override def toJson(intent: PaymentIntent, transaction: Transaction): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.paymentId, intent.paymentId.toString),
            toJsValueWrapper(Strings.transactionId, intent.transactionId.toString),
            toJsValueWrapper(Strings.gateway, intent.gateway),
            toJsValueWrapper(Strings.gatewayIntentId, intent.gatewayIntentId),
            toJsValueWrapper(Strings.resourceId, transaction.resourceId.toString),
            toJsValueWrapper(Strings.price, intent.price),
            toJsValueWrapper(Strings.surcharge, intent.surcharge),
            toJsValueWrapper(Strings.amount, intent.amount),
            toJsValueWrapper(Strings.created, transaction.created),
            toJsValueWrapper(Strings.status, intent.status.toString),
            toJsValueWrapper(Strings.discount, intent.discount),
            toJsValueWrapper(Strings.url, intent.url),
            toJsValueWrapper(Strings.created, intent.created),
            toJsValueWrapper(Strings.buyerReference, intent.buyerReference),
            toJsValueWrapper(Strings.instrument, intent.instrument),
            toJsValueWrapper(Strings.fingerprint, intent.fingerprint),
            toJsValueWrapper(Strings.returnUrlParams, intent.returnUrlParams),
            toJsValueWrapper(Strings.metadata, intent.metadata),
            toJsValueWrapper(Strings.extraInfo, intent.extraInfo),
            toJsValueWrapper(Strings.updated, intent.updated)
          ).flatten: _*
        )
      )
    }
  }
}
