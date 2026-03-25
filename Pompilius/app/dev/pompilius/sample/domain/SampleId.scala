package dev.pompilius.sample.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class SampleId(id: Long) extends SnowflakeId

object SampleId extends Snowflake {

  def apply(s: String): SampleId = SampleId(parseId(s))
  def gen(node: Int): SampleId = SampleId(genId(node))
}

