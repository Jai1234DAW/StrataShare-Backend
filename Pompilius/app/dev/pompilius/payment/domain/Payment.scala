package dev.pompilius.payment.domain

import dev.pompilius.transaction.domain.TransactionId
import org.joda.time.DateTime

case class Payment(
    id: PaymentId,
    transactionId: TransactionId,
    gateway: Gateway,
    gatewayPaymentId: String,
    // Montos
    price: BigDecimal, // Precio base del recurso
    amount: BigDecimal, // Total cobrado (= price - discount)
    discount: BigDecimal = 0, // Descuento aplicado (cupones)
    netAmount: BigDecimal, // Neto después de fees de Stripe

    // Multi-moneda
    currency: String, // EUR, USD, etc.
    exchangeRate: BigDecimal = 1.0, // Tasa de cambio (para reportes en moneda base)

    // Detalles adicionales
    //Podría tener coupones específicos más adelante para gestionar descuentos específicos,
    //Por ahora será null
    couponCode: Option[String] = None,
    buyerReference: Option[String] = None, // Email, teléfono del comprador (para Stripe)
    instrument: Option[String] = None, // "card", "wallet", "paypal", etc.
    receiptUrl: Option[String] = None, // URL del recibo de Stripe

    // Estado
    refunded: Boolean = false, // Si el pago fue reembolsado

    // Fechas
    created: DateTime,
    updated: DateTime,
    metadata: Option[String] = None
)
