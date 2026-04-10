package dev.pompilius.barter.domain.exception

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class BarterRejectedException (message:String) extends ForbiddenException(message = message)
