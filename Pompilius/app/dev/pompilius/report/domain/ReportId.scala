package dev.pompilius.report.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class ReportId(id: Long) extends SnowflakeId

object ReportId extends Snowflake {

  //Pasar de String a Long, valor interno del ID
  def apply(s: String): ReportId = ReportId(parseId(s))
  def gen(node: Int): ReportId = ReportId(genId(node))
}
