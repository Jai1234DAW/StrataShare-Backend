package dev.pompilius.auth.domain.request

import dev.pompilius.users.domain.UserPassword
case class LoginRequest(username: String, password: UserPassword)
