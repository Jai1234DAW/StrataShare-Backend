package dev.pompilius.users.domain

case class UserRoleFilter(userId: Option[UserId] = None, role: Option[Role] = None)

