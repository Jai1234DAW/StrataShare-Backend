package dev.pompilius.shared.domain

case class RequestLogId(id: Long)extends SnowflakeId

object RequestLogId extends Snowflake {
  def apply(s: String): RequestLogId = RequestLogId(parseId(s))
  def gen(node: Int): RequestLogId = RequestLogId(genId(node))
}