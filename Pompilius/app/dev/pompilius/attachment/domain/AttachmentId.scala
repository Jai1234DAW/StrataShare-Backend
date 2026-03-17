package dev.pompilius.attachments.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class AttachmentId(id: Long) extends SnowflakeId

object AttachmentId extends Snowflake {
  def apply(s: String): AttachmentId = AttachmentId(parseId(s))
  def gen(node: Int): AttachmentId = AttachmentId(genId(node))
}