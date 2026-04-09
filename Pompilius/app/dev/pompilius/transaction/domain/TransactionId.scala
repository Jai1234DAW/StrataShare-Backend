package dev.pompilius.transaction.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class TransactionId(id: Long) extends SnowflakeId

object TransactionId  extends Snowflake{
  def apply(id: String): TransactionId = TransactionId(parseId(id))
  def gen(nodeId: Int): TransactionId = TransactionId(genId(nodeId))
}

