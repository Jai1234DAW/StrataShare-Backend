package dev.pompilius.barter.domain.exception

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class BarterAlreadyCompletedException(message: String = "Barter not allowed") extends ForbiddenException(message = message)
