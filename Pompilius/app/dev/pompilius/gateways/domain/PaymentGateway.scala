package dev.pompilius.gateways.domain

import dev.pompilius.payment.domain.{PaymentCreateParams, PaymentIntent}

import scala.concurrent.Future

//Trait genérico para todas las pasarelas de pago.
// Define las operaciones comunes que debe implementar cualquier pasarela de pago.

trait PaymentGateway {

  // Tipo
  def gatewayType: Gateway

  // Para crear el intent a la pasarela de pago
  def createPaymentIntent(createParams: PaymentCreateParams): Future[PaymentIntent]

  //creo que esto es innecesario
  //def validateWebhook(payload: String, signature: String): Boolean
}
