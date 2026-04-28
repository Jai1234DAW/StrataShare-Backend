package dev.pompilius.review.domain

import dev.pompilius.resource.domain.ResourceId

case class GeneralReview(
    resourceId: ResourceId,
    totalComments: Int,
    ratingAverage: Double
)
