package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class InternalServerException(message: String = "Internal server error") extends VerboseException(message = message)