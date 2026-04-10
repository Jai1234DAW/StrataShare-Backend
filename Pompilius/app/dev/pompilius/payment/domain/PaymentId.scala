package dev.pompilius.payment.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class PaymentId(id: Long) extends SnowflakeId

object PaymentId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): PaymentId = PaymentId(parseId(s))
  def gen(node: Int): PaymentId = PaymentId(genId(node))
}
