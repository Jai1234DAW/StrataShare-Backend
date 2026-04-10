package dev.pompilius.barter.domain.exception

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class BarterNotAllowException(message: String = "Barter not allowed") extends ForbiddenException(message = message)
