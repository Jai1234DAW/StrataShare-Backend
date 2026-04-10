package dev.pompilius.payment.domain

import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.users.domain.UserId

case class PaymentFilter(
    transactionId: Option[TransactionId] = None,
    // Filtros propios de Payment
    gateway: Option[Gateway] = None,
    gatewayPaymentId: Option[String] = None,
    currency: Option[String] = None,
    instrument: Option[String] = None, // "card", "wallet", "paypal"
    couponCode: Option[String] = None,
    refunded: Option[Boolean] = None,
    withDiscount: Option[Boolean] = None, // discount > 0

    // Rangos de montos
    minAmount: Option[BigDecimal] = None,
    maxAmount: Option[BigDecimal] = None,
    // Filtros generales
    search: Option[String] = None, // Búsqueda por gatewayPaymentId, buyerReference, etc.
    userBuyer: Option[UserId] = None,
    userSeller: Option[UserId] = None
)
