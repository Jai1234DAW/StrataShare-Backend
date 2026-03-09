package dev.pompilius.auth.domain

import dev.pompilius.user.domain.UserId

case class SessionFilter(
    id: Option[SessionId] = None,
    userId: Option[UserId] = None,
    deleted: Option[Boolean] = None
)
