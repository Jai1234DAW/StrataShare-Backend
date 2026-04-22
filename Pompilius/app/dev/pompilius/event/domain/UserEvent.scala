package dev.pompilius.event.domain

import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class UserEvent(
    id: UserEventId,
    userId: UserId,
    event: EventU,
    created: DateTime
)
