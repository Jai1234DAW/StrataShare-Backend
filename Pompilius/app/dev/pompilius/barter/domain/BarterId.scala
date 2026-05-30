package dev.pompilius.barter.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}
import play.api.Logger

case class BarterId(id: Long) extends SnowflakeId

object BarterId extends Snowflake {

  private val logger = Logger(this.getClass)

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): BarterId = {
    logger.info(s"[BARTER ID PARSE] Input string (base36): $s")
    try {
      val parsedId = parseId(s)
      logger.info(s"[BARTER ID PARSE] Converted to Long: $parsedId")
      BarterId(parsedId)
    } catch {
      case e: Exception =>
        logger.error(s"[BARTER ID PARSE] Error parsing '$s': ${e.getMessage}", e)
        throw e
    }
  }

  def gen(node: Int): BarterId = BarterId(genId(node))
}
