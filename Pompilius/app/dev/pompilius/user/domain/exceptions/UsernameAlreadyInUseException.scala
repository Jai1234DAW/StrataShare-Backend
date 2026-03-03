package dev.pompilius.user.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class UsernameAlreadyInUseException(message: String = "This username is already being used by another user")
  extends VerboseException(message = message)
