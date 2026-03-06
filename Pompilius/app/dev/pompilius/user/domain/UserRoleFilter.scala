package dev.pompilius.user.domain

case class UserRoleFilter(userId: Option[UserId] = None, role: Option[Role] = None)

