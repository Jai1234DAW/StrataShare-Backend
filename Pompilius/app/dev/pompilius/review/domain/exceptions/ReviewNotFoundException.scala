package dev.pompilius.review.domain.exceptions

import dev.pompilius.review.domain.ReviewId
import dev.pompilius.shared.domain.VerboseException

class ReviewNotFoundException(message: String = "Review not found") extends VerboseException(message = message)

object ReviewNotFoundException {
  def apply(reviewId: ReviewId): ReviewNotFoundException = {
    new ReviewNotFoundException(s"Sample with id=${reviewId.toString} not found")
  }
}
