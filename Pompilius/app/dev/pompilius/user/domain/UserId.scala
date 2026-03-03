package dev.pompilius.user.domain

import dev.pompilius.shared.domain.{Snowflake, SnowflakeId}

case class UserId(id: Long) extends SnowflakeId

object UserId extends Snowflake {
    def apply(s: String): UserId = UserId(parseId(s))
    def gen(node: Int): UserId = UserId(genId(node))
}