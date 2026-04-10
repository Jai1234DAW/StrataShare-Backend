package dev.pompilius.payment.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class PaymentCreateException(message: String = "Error creating payment")
    extends VerboseException(message = message, logAsError = true)
