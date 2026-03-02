package dev.pompilius.shared.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class BadRequestException(message: String="Bad request") extends VerboseException(message=message)