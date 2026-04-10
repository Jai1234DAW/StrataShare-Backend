package dev.pompilius.payment.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class PaymentIntentId(id: Long) extends SnowflakeId

object PaymentIntentId extends Snowflake {

  def apply(s: String): PaymentIntentId = PaymentIntentId(parseId(s))
  def gen(node: Int): PaymentIntentId = PaymentIntentId(genId(node))
}

