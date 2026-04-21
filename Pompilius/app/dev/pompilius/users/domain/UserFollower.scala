package dev.pompilius.users.domain

import org.joda.time.DateTime

case class UserFollower(
    userId: UserId,
    followerId: UserId,
    followedAt: DateTime
)
