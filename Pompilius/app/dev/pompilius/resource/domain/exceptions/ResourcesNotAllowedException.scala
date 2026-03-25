package dev.pompilius.resource.domain.exceptions

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class ResourcesNotAllowedException(message: String = "Not Allowed To Use") extends ForbiddenException(message = message)