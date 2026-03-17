package dev.pompilius.users.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class UserId(id: Long) extends SnowflakeId

object UserId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): UserId = UserId(parseId(s))
  def gen(node: Int): UserId = UserId(genId(node))
}
