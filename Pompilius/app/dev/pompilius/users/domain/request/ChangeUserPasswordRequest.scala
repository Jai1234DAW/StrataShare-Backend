package dev.pompilius.users.domain.request

case class ChangeUserPasswordRequest(oldPassword: String, newPassword: String, closeAllSessions: Boolean)
