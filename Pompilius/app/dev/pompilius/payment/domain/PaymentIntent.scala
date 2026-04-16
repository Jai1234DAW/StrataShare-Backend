package dev.pompilius.payment.domain

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.transaction.domain.TransactionId
import org.joda.time.DateTime

/**
  * PaymentIntent representa el INTENTO de pago.
  * Se crea cuando el usuario inicia el proceso de pago.
  * Puede tener diferentes estados (PENDING, SUCCEEDED, FAILED, etc.)
  *
  * Relación: PaymentIntent → Transaction (vía transactionId)
  * No necesita buyerId/sellerId/resourceId porque están en Transaction
  */
case class PaymentIntent(
    paymentId: PaymentId,
    transactionId: TransactionId, // FK a Transaction
    gateway: Gateway,
    gatewayIntentId: String, // ID del intent en la pasarela (ej: pi_xxx de Stripe)
    resourceId: ResourceId,
    price: BigDecimal, // Precio base del recurso
    //Fee de la pasarela, se calcula al crear el intent (ej: 2.9% + 0.30 USD en Stripe)
    surcharge: BigDecimal,
    amount: BigDecimal, // Total a cobrar (price + fee + surcharge - discount)
    status: PaymentIntentStatus, // Estado del intent (PENDING → SUCCEEDED/FAILED)
    //couponCode: Option[String] = None,
    discount: Option[BigDecimal] = None,
    url: Option[String] = None, // URL de pago si aplica
    created: DateTime,
    buyerReference: Option[String] = None, // Email del comprador
    instrument: Option[String] = None,
    fingerprint: Option[String] = None,
    returnUrlParams: Option[Map[String, String]] = None,
    metadata: Option[String] = None,
    extraInfo: Option[String] = None,
    updated: Option[DateTime] = None
)
