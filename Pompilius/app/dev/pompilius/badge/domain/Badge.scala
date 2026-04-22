package dev.pompilius.badge.domain

import org.joda.time.DateTime

case class Badge(
    id: BadgeId,
    badgeType: BadgeType,
    name: String,
    description: String,
    imageUrl: Option[String],
    created: DateTime
)
