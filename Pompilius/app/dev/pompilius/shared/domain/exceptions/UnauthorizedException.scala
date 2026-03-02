package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class UnauthorizedException(message: String) extends VerboseException(message = message)