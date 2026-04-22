package dev.pompilius.badge.domain

import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class UserBadge(
    userId: UserId,
    badgeId: BadgeId,
    earnedAt: DateTime
)


