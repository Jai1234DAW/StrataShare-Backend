package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class ForbiddenException(message: String = "Forbidden") extends VerboseException(message = message)