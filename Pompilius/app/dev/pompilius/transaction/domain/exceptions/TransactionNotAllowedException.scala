package dev.pompilius.transaction.domain.exceptions

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class TransactionNotAllowedException(message: String = "Resource not found") extends ForbiddenException(message = message)

