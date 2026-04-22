package dev.pompilius.event.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class EventId(id: Long) extends SnowflakeId

object EventId extends Snowflake {
  def apply(s: String): EventId = EventId(parseId(s))
  def gen(node: Int): EventId = EventId(genId(node))
}

// Esto posiblemente se elimine