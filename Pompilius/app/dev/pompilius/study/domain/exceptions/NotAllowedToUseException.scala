package dev.pompilius.study.domain.exceptions

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class NotAllowedToUseException(message: String = "Not Allowed To Use") extends ForbiddenException(message = message)