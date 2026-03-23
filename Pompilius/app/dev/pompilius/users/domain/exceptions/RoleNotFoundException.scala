package dev.pompilius.users.domain.exceptions

class RoleNotFoundException(message: String = "Role not found") extends VerboseException(message = message)

object RoleNotFoundException {
  def apply(roleId: RoleId): RoleNotFoundException = {
    new RoleNotFoundException(s"Role with id=${roleId.toString} not found")
  }
}
