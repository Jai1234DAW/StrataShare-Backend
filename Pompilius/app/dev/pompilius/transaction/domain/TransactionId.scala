package dev.pompilius.transaction.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}
import play.api.Logger

case class TransactionId(id: Long) extends SnowflakeId

object TransactionId  extends Snowflake{
  private val logger = Logger(this.getClass)

  def apply(id: String): TransactionId = {
    logger.info(s"[TRANSACTION ID PARSE] Input string (base36): $id")
    try {
      val parsedId = parseId(id)
      logger.info(s"[TRANSACTION ID PARSE] Converted to Long: $parsedId")
      TransactionId(parsedId)
    } catch {
      case e: Exception =>
        logger.error(s"[TRANSACTION ID PARSE] Error parsing '$id': ${e.getMessage}", e)
        throw e
    }
  }

  def gen(nodeId: Int): TransactionId = TransactionId(genId(nodeId))
}

