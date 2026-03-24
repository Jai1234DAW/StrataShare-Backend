package dev.pompilius.mail.domain

import dev.pompilius.shared.domain.Snowflake
import dev.pompilius.shared.domain.SnowflakeId

case class MailSentId(id: Long) extends SnowflakeId

object MailSentId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): MailSentId = MailSentId(parseId(s))
  def gen(node: Int): MailSentId = MailSentId(genId(node))
}
