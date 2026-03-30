package dev.pompilius.study.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class StudyId(id: Long) extends SnowflakeId
object StudyId extends Snowflake {
  def apply(s: String): StudyId = StudyId(parseId(s))
  def gen(node: Int): StudyId = StudyId(genId(node))
}
