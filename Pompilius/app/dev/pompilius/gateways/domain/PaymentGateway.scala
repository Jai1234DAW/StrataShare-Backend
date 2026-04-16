package dev.pompilius.gateways.domain

import dev.pompilius.payment.domain.{PaymentCreateParams, PaymentIntent}

import scala.concurrent.Future

/**
  * Trait genérico para todas las pasarelas de pago.
  * Define las operaciones comunes que debe implementar cualquier pasarela de pago.
  *
  * Implementaciones:
  * - StripePaymentGateway: Para Stripe
  * - PaypalPaymentGateway: Para PayPal (futura)
  * - etc.
  */
trait PaymentGateway {

  // Tipo
  def gatewayType: Gateway

  // Para crear el intent a la pasarela de pago
  def createPaymentIntent(createParams: PaymentCreateParams): Future[PaymentIntent]

 // def getPaymentIntent(gatewayIntentId: String): Future[PaymentIntent]

  //def getPayment(gatewayPaymentId: String): Future[GatewayPaymentIntentResponse]

  // Obtiene el estado de un PaymentIntent en la pasarela
  //def getPaymentIntentStatus(gatewayIntentId: String): Future[PaymentIntentStatus]

  // Cancela un PaymentIntent en la pasarela
  //def cancelPaymentIntent(gatewayIntentId: String): Future[Unit]

  /**
    * Valida un webhook de la pasarela
    *
    * @param payload Cuerpo del webhook
    * @param signature Firma del webhook
    * @return true si el webhook es válido
    */
  def validateWebhook(payload: String, signature: String): Boolean
}
