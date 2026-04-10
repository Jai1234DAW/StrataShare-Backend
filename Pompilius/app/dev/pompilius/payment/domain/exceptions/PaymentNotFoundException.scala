package dev.pompilius.payment.domain.exceptions

import dev.pompilius.payment.domain.PaymentId
import dev.pompilius.shared.domain.VerboseException

class PaymentNotFoundException(message: String = "Payment not found") extends VerboseException(message = message)

object PaymentNotFoundException {
  def apply(paymentId: PaymentId): PaymentNotFoundException = {
    new PaymentNotFoundException(s"Payment with id=${paymentId.toString} not found")
  }
}