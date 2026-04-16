package dev.pompilius.payment.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class GatewayException(message: String = "An error occurred in the payment gateway")
    extends VerboseException(message = message, logAsError = true)
