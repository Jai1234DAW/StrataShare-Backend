package dev.pompilius.review.domain

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime


case class Review(
    id: ReviewId,
    userId: UserId,
    resourceId: ResourceId,
    rating: Int,
    comment: Option[String],
    createdAt: DateTime,
    updatedAt: DateTime
)
