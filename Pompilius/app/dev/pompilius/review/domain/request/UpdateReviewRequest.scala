package dev.pompilius.review.domain.request

case class UpdateReviewRequest(
    rating: Option[Int],
    comment: Option[String]
)
