package dev.pompilius.study.domain

import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class StudyFilter(
    name: Option[String] = None,
    startDate: Option[DateTime] = None,
    endDate: Option[DateTime] = None,
    area: Option[Area] = None,
    authors: Option[String] = None,
    search: Option[String] = None,
    userId: Option[UserId] = None
)

