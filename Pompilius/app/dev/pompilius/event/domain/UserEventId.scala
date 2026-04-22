package dev.pompilius.event.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class UserEventId(id: Long) extends SnowflakeId

object UserEventId extends Snowflake {
  def apply(s: String): UserEventId = UserEventId(parseId(s))
  def gen(node: Int): UserEventId = UserEventId(genId(node))
}
