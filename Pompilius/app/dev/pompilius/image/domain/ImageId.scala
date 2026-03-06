package dev.pompilius.image.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class ImageId(id: Long) extends SnowflakeId

object ImageId extends Snowflake {
  def apply(s: String): ImageId = ImageId(parseId(s))
  def gen(node: Int): ImageId = ImageId(genId(node))
}
