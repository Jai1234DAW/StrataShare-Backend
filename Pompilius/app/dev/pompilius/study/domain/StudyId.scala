package dev.pompilius.studies.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class StudyId(id: Long) extends SnowflakeId

object StudyId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): StudyId = StudyId(parseId(s))
  def gen(node: Int): StudyId = StudyId(genId(node))
}

