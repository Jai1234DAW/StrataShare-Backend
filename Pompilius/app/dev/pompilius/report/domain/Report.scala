package dev.pompilius.report.domain

import dev.pompilius.users.domain.UserId

case class Report(
    id: ReportId,
    name: String,
    title: Option[String],
    authorizedUsers: List[UserId],
    sheets: Seq[Sheet]
)
