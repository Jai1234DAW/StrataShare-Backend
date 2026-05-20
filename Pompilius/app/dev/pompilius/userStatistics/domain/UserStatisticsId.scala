package dev.pompilius.userStatistics.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class UserStatisticsId(id: Long) extends SnowflakeId

object UserStatisticsId extends Snowflake {
  def apply(s: String): UserStatisticsId = UserStatisticsId(parseId(s))
  def gen(node: Int): UserStatisticsId = UserStatisticsId(genId(node))
}