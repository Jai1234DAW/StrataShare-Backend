package dev.pompilius.badge.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class BadgeId(id: Long) extends SnowflakeId

object BadgeId extends Snowflake {
  def apply(s: String): BadgeId = BadgeId(parseId(s))
  def gen(node: Int): BadgeId = BadgeId(genId(node))
}