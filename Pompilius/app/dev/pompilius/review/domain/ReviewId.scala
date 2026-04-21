package dev.pompilius.review.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class ReviewId(id: Long) extends SnowflakeId

object ReviewId extends Snowflake {
  def apply(s: String): ReviewId = ReviewId(parseId(s))
  def gen(node: Int): ReviewId = ReviewId(genId(node))
}
