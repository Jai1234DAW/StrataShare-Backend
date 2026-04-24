package dev.pompilius.study.domain

import dev.pompilius.users.domain.UserId
import dev.pompilius.shared.domain.Visibility

case class StudyFilter(
    name: Option[String] = None,
    year: Option[Int] = None,
    area: Option[Area] = None,
    authors: Option[String] = None,
    search: Option[String] = None,
    visibility: Option[Visibility] = None,
    location: Option[String] = None,
    userId: Option[UserId] = None
)
