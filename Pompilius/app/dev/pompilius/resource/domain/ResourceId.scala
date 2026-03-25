package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class ResourceId(id: Long) extends SnowflakeId

object ResourceId extends Snowflake {
  def apply(s: String): ResourceId = ResourceId(parseId(s))
  def gen(node: Int): ResourceId = ResourceId(genId(node))
}

