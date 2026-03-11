package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class TooManyRequestsException(message: String = "Too many requests", val retryAfter: Long) extends VerboseException(message = message)