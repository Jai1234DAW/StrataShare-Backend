package dev.pompilius.payment.domain

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.transaction.domain.TransactionId
import org.joda.time.DateTime

//Mirar esto
/**
 * Payment representa un pago COMPLETADO y CONFIRMADO.
 * Solo se crea cuando el PaymentIntent tiene status = SUCCEEDED.
 *
 * Diferencias con PaymentIntent:
 * - PaymentIntent: Tracking del PROCESO de pago (puede cambiar de estado)
 * - Payment: Registro FINAL del pago exitoso (inmutable, solo datos confirmados)
 *
 * Relación: Payment → Transaction (vía transactionId)
 * No necesita buyerId/sellerId/resourceId porque están en Transaction
 */
case class Payment(
    id: PaymentId,
    transactionId: TransactionId, // FK a Transaction (1:1)
    gateway: Gateway, // Pasarela usada
    gatewayPaymentId: String, // ID del pago en la pasarela (mismo que gatewayIntentId)
    amount: BigDecimal, // Monto inicial del pago (antes de fees y ajustes)
    netAmount: BigDecimal, // Monto neto después de fees de la pasarela + todos los ajustes
    currency: String,
    receiptUrl: Option[String] = None, // URL del recibo
    instrument: Option[String] = None, // Método de pago usado
    refunded: Boolean = false, // Si fue reembolsado
    refundedAmount: BigDecimal = 0,
    created: DateTime,
    updated: DateTime,
    metadata: Option[String] = None
)
