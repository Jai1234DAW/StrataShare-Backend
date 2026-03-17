package dev.pompilius.users.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.users.domain.UserId

class UserNotFoundException(message: String = "User not found") extends VerboseException(message = message)

object UserNotFoundException {
  def apply(userId: UserId): UserNotFoundException = {
    new UserNotFoundException(s"User with id=${userId.toString} not found")
  }
}
