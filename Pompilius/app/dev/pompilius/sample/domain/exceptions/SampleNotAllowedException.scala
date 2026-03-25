package dev.pompilius.sample.domain.exceptions

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class SampleNotAllowedException(message: String = "Not Allowed To Use") extends ForbiddenException(message = message)