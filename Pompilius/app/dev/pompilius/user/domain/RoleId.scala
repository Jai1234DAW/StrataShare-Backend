package dev.pompilius.user.domain

import dev.pompilius.shared.domain.{Snowflake,SnowflakeId}

case class RoleId(id: Long) extends SnowflakeId

object RoleId extends Snowflake {
  def apply(s: String): RoleId = RoleId(parseId(s))
  def gen(node: Int): RoleId = RoleId(genId(node))
}

