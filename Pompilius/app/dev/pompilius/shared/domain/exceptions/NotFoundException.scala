package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class NotFoundException(message: String = "Not Found") extends VerboseException(message = message)