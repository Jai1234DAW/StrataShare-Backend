package dev.pompilius.barter.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class BarterId(id: Long) extends SnowflakeId

object BarterId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): BarterId = BarterId(parseId(s))
  def gen(node: Int): BarterId = BarterId(genId(node))
}
