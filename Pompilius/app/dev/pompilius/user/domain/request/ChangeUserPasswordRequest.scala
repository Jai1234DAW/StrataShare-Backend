package dev.pompilius.user.domain.request

case class ChangeUserPasswordRequest (oldPassword:String,
                                      newPassword:String,
                                      closeAllSessions: Boolean,)

