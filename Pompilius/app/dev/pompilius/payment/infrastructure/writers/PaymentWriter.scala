package dev.pompilius.payment.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.payment.domain.{Payment, PaymentIntent}
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.transaction.domain.Transaction
import jakarta.inject.Singleton
import play.api.Configuration
import play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PaymentWriterImpl])
trait PaymentWriter {
  def toJson(transaction: Transaction, payment: Payment): Future[JsValue]
  def asBuyer(transaction: Transaction, payment: Payment): Future[JsValue]
  def asSeller(transaction: Transaction, payment: Payment): Future[JsValue]
  def asAdmin(transaction: Transaction, payment: Payment): Future[JsValue]
}

@Singleton
class PaymentWriterImpl @Inject() (configuration: Configuration)(implicit ec: ExecutionContext) extends PaymentWriter {

  override def toJson(transaction: Transaction, payment: Payment): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.paymentId, payment.id.toString),
            toJsValueWrapper(Strings.transactionId, payment.transactionId.toString),
            toJsValueWrapper(Strings.resourceId, transaction.resourceId.toString),
            toJsValueWrapper(Strings.gateway, payment.gateway),
            toJsValueWrapper(Strings.netAmount, payment.netAmount),
            toJsValueWrapper(Strings.amount, payment.amount),
            toJsValueWrapper(Strings.instrument, payment.instrument)
          ).flatten: _*
        )
      )
    }
  }

  def asBuyer(transaction: Transaction, payment: Payment): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, payment)
    } yield {
      val finalJson = baseJson.as[JsObject] ++ Json.obj(
        List(
          toJsValueWrapper((Strings.amount, payment.amount)), // Lo que pagó el comprador
          toJsValueWrapper((Strings.currency, payment.currency)),
          toJsValueWrapper((Strings.created, payment.created))
        ).flatten: _*
      )

      finalJson
    }
  }

  def asSeller(transaction: Transaction, payment: Payment): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, payment)
    } yield {
      val finalJson = baseJson.as[JsObject] ++ Json.obj(
        List(
          toJsValueWrapper((Strings.amount, payment.amount)), // Total del pago
          toJsValueWrapper((Strings.platformFee, payment.platformFee)), // Comisión de la plataforma
          toJsValueWrapper((Strings.gatewayFee, payment.gatewayFee)), // Fee de Stripe
          toJsValueWrapper((Strings.netAmount, payment.netAmount)), // Lo que recibe el vendedor
          toJsValueWrapper((Strings.currency, payment.currency)),
          toJsValueWrapper((Strings.created, payment.created))
        ).flatten: _*
      )
      finalJson
    }
  }

  def asAdmin(transaction: Transaction, payment: Payment): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, payment)
    } yield {
      val finalJson = baseJson.as[JsObject] ++ Json.obj(
        List(
          toJsValueWrapper((Strings.amount, payment.amount)),
          toJsValueWrapper((Strings.platformFee, payment.platformFee)),
          toJsValueWrapper((Strings.gatewayFee, payment.gatewayFee)),
          toJsValueWrapper((Strings.netAmount, payment.netAmount)),
          toJsValueWrapper((Strings.currency, payment.currency)),
          toJsValueWrapper((Strings.refunded, payment.refunded)),
          toJsValueWrapper((Strings.refundedAmount, payment.refundedAmount)),
          toJsValueWrapper((Strings.created, payment.created)),
          toJsValueWrapper((Strings.updated, payment.updated)),
          toJsValueWrapper((Strings.metadata, payment.metadata))
        ).flatten: _*
      )
      finalJson
    }
  }
}
