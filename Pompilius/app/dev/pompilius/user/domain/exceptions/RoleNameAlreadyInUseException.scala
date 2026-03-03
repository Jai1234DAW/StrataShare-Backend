package dev.pompilius.user.domain.exceptions

import dev.pompilius.shared.domain.VerboseException

class RoleNameAlreadyInUseException(message: String = "This name is already being used by another role")
  extends VerboseException(message = message)