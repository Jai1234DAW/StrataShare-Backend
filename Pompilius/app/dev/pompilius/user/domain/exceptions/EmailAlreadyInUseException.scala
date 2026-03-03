package dev.pompilius.user.domain.exceptions
import dev.pompilius.shared.domain.VerboseException

class EmailAlreadyInUseException(message: String = "Email is already in use")
    extends VerboseException(message = message)
