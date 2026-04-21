package dev.pompilius.review.domain.request

case class CreateReviewRequest(
    resourceId: String,
    rating: Int,
    comment: Option[String]
)
