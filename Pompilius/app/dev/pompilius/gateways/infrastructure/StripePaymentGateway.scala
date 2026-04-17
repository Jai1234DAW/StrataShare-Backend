package dev.pompilius.gateways.infrastructure

import com.stripe.Stripe
import com.stripe.model.checkout.Session
import com.stripe.net.Webhook.constructEvent
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.ProductData
import dev.pompilius.Strings
import dev.pompilius.gateways.domain.{Gateway, PaymentGateway}
import dev.pompilius.payment.domain._
import dev.pompilius.gateways.infrastructure.controllers.routes
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.shared.infrastructure.writers.RequestFingerprintWriter
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

//Implementación de PaymentGateway para Stripe usando la librería oficial de Stripe.
// Crea un Session de Stripe para el pago y devuelve la URL para redirigir al usuario a Stripe Checkout.
// Valida los webhooks de Stripe usando la librería oficial.

@Singleton
class StripePaymentGateway @Inject() (
    configuration: Configuration,
    fingerprintWriter: RequestFingerprintWriter,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends PaymentGateway {

  private val logger = Logger(this.getClass)

  private val stripeConfig = configuration.stripe
  private val secretKey = stripeConfig.secretKey
  private val webhookSecret = stripeConfig.webhookSecret
  private val isSandbox = stripeConfig.sandbox

  override def gatewayType: Gateway = Gateway.STRIPE

  Stripe.apiKey = configuration.stripe.secretKey

  override def createPaymentIntent(createParams: PaymentCreateParams): Future[PaymentIntent] = {



    for {
      fingerprint <- createParams.fingerprint match {
        case Some(f) =>
          fingerprintWriter.toJson(f).map(Some(_))
        case _ =>
          Future.successful(None)
      }

      language = createParams.fingerprint.flatMap(_.getLang).map(_.language.toUpperCase)

      baseUrl = configuration.baseUrl

      okUrl =
        baseUrl + routes.GatewayController
          .oneTimePaymentCompleted(Gateway.STRIPE.toString, createParams.id.toString)
          .url

      session <- Future {
        val sessionCreateParams = SessionCreateParams
          .builder()
          .setClientReferenceId("pi_" + createParams.id.toString)
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(okUrl)
          .setCancelUrl(
            baseUrl + routes.GatewayController
              .oneTimePaymentCanceled(Gateway.STRIPE.toString, createParams.id.toString)
              .url
          )
          .addLineItem(
            SessionCreateParams.LineItem
              .builder()
              .setQuantity(1)
              .setPriceData {
                val priceData = PriceData
                  .builder()
                  .setProductData(
                    ProductData
                      .builder()
                      .setName(createParams.resource.id.toString)
                      .putMetadata(Strings.paymentId, createParams.id.toString)
                      .putMetadata(Strings.userId, createParams.buyer.id.toString)
                      .putMetadata(Strings.sellerId, createParams.seller.id.toString)
                      .build
                  )
                  .setCurrency(configuration.stripe.currency)
                  .setUnitAmountDecimal((createParams.amount * 100).setScale(0, RoundingMode.DOWN).bigDecimal)
                priceData.build()
              }
              .build()
          )
          .putMetadata(Strings.paymentId, createParams.id.toString)
          .putMetadata(Strings.userId, createParams.buyer.id.toString)
          .putMetadata(Strings.sellerId, createParams.seller.id.toString)
          .putMetadata(Strings.resourceId, createParams.resource.id.toString)
          //.putMetadata(Strings.couponCode, createParams.couponCode.getOrElse(""))
          .putMetadata(Strings.discount, createParams.discount.toString)

        language.foreach { lang =>
          Try(SessionCreateParams.Locale.valueOf(lang)).foreach(
            sessionCreateParams.setLocale
          )
        }

        Session.create(sessionCreateParams.build())
      }
    } yield {

      PaymentIntent(
        paymentId = createParams.id,
        transactionId = createParams.transactionId,
        gateway = Gateway.STRIPE,
        gatewayIntentId = session.getId,
        resourceId = createParams.resource.id,
        price = createParams.price,
        surcharge = createParams.surcharge,
        amount = createParams.amount,
        status =
          PaymentIntentStatus.REQUIRES_PAYMENT_METHOD, // El estado inicial siempre es REQUIRES_PAYMENT_METHOD, luego se actualizará con el webhook
        //couponCode = createParams.couponCode,
        discount = Some(createParams.discount),
        url = Some(session.getUrl),
        created = clock.now,
        buyerReference = None,
        instrument = None,
        fingerprint = fingerprint.map(_.toString),
        returnUrlParams = createParams.returnUrlParams,
        metadata = Try(session.getRawJsonObject.toString).toOption,
        extraInfo = createParams.extraInfo
      )
    }
  }

//  override def validateWebhook(payload: String, signature: String): Boolean = {
//    try {
//      // Usar la librería de Stripe para validar el webhook con HMAC SHA256
//      val event = constructEvent(payload, signature, webhookSecret)
//      logger.info(s"Webhook validated successfully: ${event.getType}")
//      true
//    } catch {
//      case e: com.stripe.exception.SignatureVerificationException =>
//        logger.error(s"Invalid webhook signature: ${e.getMessage}")
//        false
//      case e: Exception =>
//        logger.error(s"Error validating webhook: ${e.getMessage}")
//        false
//    }
//  }

//  // Mapea los estados de Stripe a los estados interno
//  private def mapStripeStatusToInternal(stripeStatus: String): PaymentIntentStatus = {
//    stripeStatus match {
//      case "requires_payment_method" => PaymentIntentStatus.REQUIRES_PAYMENT_METHOD
//      case "requires_confirmation"   => PaymentIntentStatus.REQUIRES_CONFIRMATION
//      case "requires_action"         => PaymentIntentStatus.REQUIRES_ACTION
//      case "processing"              => PaymentIntentStatus.PROCESSING
//      case "succeeded"               => PaymentIntentStatus.SUCCEEDED
//      case "canceled"                => PaymentIntentStatus.CANCELED
//      case _                         => PaymentIntentStatus.REQUIRES_PAYMENT_METHOD
//    }
//  }
}
