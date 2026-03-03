package dev.pompilius.user.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.user.domain.RoleId

class RoleNotFoundException(message: String = "Role not found") extends VerboseException(message = message)

object RoleNotFoundException {
  def apply(roleId: RoleId): RoleNotFoundException = {
    new RoleNotFoundException(s"Role with id=${roleId.toString} not found")
  }
}
