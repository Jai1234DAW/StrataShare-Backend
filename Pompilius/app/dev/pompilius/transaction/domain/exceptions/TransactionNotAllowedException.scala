package dev.pompilius.transaction.domain.exceptions

import dev.pompilius.shared.domain.exceptions.ForbiddenException

class TransactionNotAllowedException(message: String = "Transaction Not Allowed") extends ForbiddenException(message = message)

