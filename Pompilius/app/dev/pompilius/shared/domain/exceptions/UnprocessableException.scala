package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class UnprocessableException(message: String) extends VerboseException(message = message)