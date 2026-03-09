package dev.pompilius.auth.domain.request

case class PasswordResetRequest(newPassword: String, token: String, closeAllSessions: Boolean)