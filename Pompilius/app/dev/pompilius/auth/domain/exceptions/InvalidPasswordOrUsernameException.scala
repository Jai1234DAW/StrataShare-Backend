package dev.pompilius.auth.domain.exceptions

import dev.pompilius.shared.domain.exceptions.UnauthorizedException

class InvalidPasswordOrUsernameException(message:String="Invalid password or username") extends UnauthorizedException(message)

