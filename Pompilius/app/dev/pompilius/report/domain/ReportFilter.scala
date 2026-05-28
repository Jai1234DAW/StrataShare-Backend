package dev.pompilius.report.domain

import dev.pompilius.users.domain.UserId

case class ReportFilter(
    name: Option[String],
    userId: Option[UserId]
)
