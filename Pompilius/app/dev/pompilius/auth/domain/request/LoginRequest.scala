package dev.pompilius.auth.domain.request

import dev.pompilius.user.domain.UserPassword
case class LoginRequest(username: String, password: UserPassword)
