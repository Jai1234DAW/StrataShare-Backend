package dev.pompilius.auth.domain.exceptions

import dev.pompilius.shared.domain.exceptions.UnauthorizedException

class InvalidPasswordOrUserNameException (message:String="Incorrect username or password") extends UnauthorizedException(message)

