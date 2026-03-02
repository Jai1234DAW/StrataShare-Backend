package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class NotFoundException(message: String = "Forbidden") extends VerboseException(message = message)