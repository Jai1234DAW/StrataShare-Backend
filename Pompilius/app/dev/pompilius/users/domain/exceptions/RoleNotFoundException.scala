package dev.pompilius.users.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.users.domain.Role

class RoleNotFoundException(message: String = "Role not found") extends VerboseException(message = message)

object RoleNotFoundException {
  def apply(role: Role): RoleNotFoundException = {
    new RoleNotFoundException(s"Role with id=${role.description} not found")
  }
}
