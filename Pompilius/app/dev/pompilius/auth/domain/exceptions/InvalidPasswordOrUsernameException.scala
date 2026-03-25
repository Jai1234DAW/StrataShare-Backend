package dev.pompilius.auth.domain.exceptions

import dev.pompilius.shared.domain.exceptions.UnauthorizedException

class InvalidPasswordOrUsernameException extends UnauthorizedException(message)

