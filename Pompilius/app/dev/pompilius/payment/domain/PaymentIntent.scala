package dev.pompilius.payment.domain

import org.joda.time.DateTime

case class PaymentIntent(
    paymentId: PaymentId,
    gateway: Gateway,
    gatewayIntentId: String,
    amount: BigDecimal,
    currency: String,
    status: PaymentIntentStatus,
    created: DateTime,
    updated: DateTime,
    fingerprint: String,
    metadata: Option[String] = None
)
