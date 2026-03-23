package dev.pompilius.shared.domain

import org.joda.time.DateTime
import dev.pompilius.users.domain.UserId

case class RequestLog(
    id: RequestLogId,
    userId: UserId,
    timestamp: DateTime,
    address: String,
    method: String,
    path: String,
    body: Option[String],
    metadata: Option[String]
)
