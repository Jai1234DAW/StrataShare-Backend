package dev.pompilius.user.domain.exceptions

import dev.pompilius.shared.domain.VerboseException


class RoleInUseException(message: String = "This role cannot be deleted because it is assigned to at least one user")
  extends VerboseException(message = message)
