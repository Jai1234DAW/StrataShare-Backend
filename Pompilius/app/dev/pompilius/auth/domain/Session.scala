package dev.pompilius.auth.domain

import dev.pompilius.country.domain.Country
import org.joda.time.DateTime
import dev.pompilius.users.domain.UserId

case class Session(
    id: SessionId,
    userId: UserId,
    deleted: Boolean,
    created: DateTime,
    address: String,
    userAgent: Option[String] = None,
    country: Option[Country] = None
)
