package dev.pompilius.auth.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class SessionId(id: Long) extends SnowflakeId

object SessionId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): SessionId = SessionId(parseId(s))
  def gen(node: Int): SessionId = SessionId(genId(node))
}
