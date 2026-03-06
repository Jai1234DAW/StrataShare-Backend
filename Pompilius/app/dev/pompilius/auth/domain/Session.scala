package dev.pompilius.auth.domain

import org.apache.pekko.http.scaladsl.model.DateTime
import dev.pompilius.user.domain.UserId

case class Session(
    token: SessionToken,
    userId: UserId,
    deleted: Boolean,
    created: DateTime,
    address: String,
    userAgent: Option[String] = None,
    country: Option[String] = None
)
