package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class InternalServerException(message: String = "Not found") extends VerboseException(message = message)