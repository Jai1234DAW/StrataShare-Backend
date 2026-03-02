package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class TooManyRequestsException(message: String = "Too many requests") extends VerboseException(message = message)