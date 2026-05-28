package dev.pompilius.payment.application

import com.google.inject.ImplementedBy
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.gateways.infrastructure.PaymentGatewayFactory
import dev.pompilius.payment.domain._
import dev.pompilius.payment.domain.exceptions.PaymentCreateException
import dev.pompilius.payment.domain.request.CreatePaymentRequest
import dev.pompilius.shared.domain.{Configuration, RequestFingerprint, VerboseException}
import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.users.domain.User
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import scala.util.control.NonFatal

/**
  * Servicio de pagos que maneja la lógica de negocio para:
  * - Validar compras
  * - Crear PaymentIntents usando la pasarela apropiada
  */
@ImplementedBy(classOf[PaymentCreatorImpl])
trait PaymentCreator {
  def createPaymentIntent(
      request: CreatePaymentRequest,
      buyer: User,
      transactionId: TransactionId,
      requestFingerprint: RequestFingerprint,
      purchaseData: PurchaseData
  ): Future[PaymentIntent]
}

@Singleton
class PaymentCreatorImpl @Inject() (
    paymentGatewayFactory: PaymentGatewayFactory,
    configuration: Configuration
)(implicit ec: ExecutionContext)
    extends PaymentCreator {

  private val logger = Logger(this.getClass)

  override def createPaymentIntent(
      request: CreatePaymentRequest,
      buyer: User,
      transactionId: TransactionId,
      requestFingerprint: RequestFingerprint,
      purchaseData: PurchaseData
  ): Future[PaymentIntent] = {

    //Aquí iría la lógica para el cupon de descuento, que de momento no será implementada, por lo que el descuento se queda en 0
    val discount = BigDecimal(0)
    val priceWithDiscount = purchaseData.totalAmount - discount

    val surcharge = request.gateway match {
      case Gateway.STRIPE =>
        configuration.gatewaySurcharges.stripeSurchargePercentage
      case _ =>
        BigDecimal(0)
    }

    val amountWithSurcharge =
      if (surcharge > 0) {
        ((priceWithDiscount * (100 + surcharge)) / 100).setScale(2, RoundingMode.UP)
      } else {
        priceWithDiscount
      }

    val paymentId = PaymentId.gen(configuration.nodeId)
    val gatewayImpl = paymentGatewayFactory.getGateway(request.gateway)

    val createParams = PaymentCreateParams(
      id = paymentId,
      transactionId = transactionId,
      gateway = request.gateway,
      instrument = request.instrument,
      buyer = purchaseData.buyer,
      seller = purchaseData.seller,
      resource = purchaseData.resource,
      price = priceWithDiscount,
      amount = amountWithSurcharge,
      surcharge = surcharge,
      // couponCode = request.couponCode,
      discount = discount,
      fingerprint = Some(requestFingerprint),
      returnUrlParams = request.returnUrlParams,
      extraInfo = request.extraInfo
    )

    gatewayImpl
      .createPaymentIntent(createParams)
      .recoverWith {
        case e: VerboseException =>
          logger.error(s"Error creating payment intent (@${e.id}): ${e.getMessage}", e)
          Future.failed(e)

        case NonFatal(e) =>
          val verboseException = new PaymentCreateException()
          logger.error(
            s"Error creating payment intent (@${verboseException.id}): ${e.getMessage}",
            e
          )
          Future.failed(verboseException)
      }
  }
}
