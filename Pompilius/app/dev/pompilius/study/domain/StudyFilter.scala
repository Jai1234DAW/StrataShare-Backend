package dev.pompilius.studies.domain

import org.joda.time.DateTime

case class StudyFilter(
    name: Option[String] = None,
    visibility: Option[String] = None,
    localization: Option[String] = None,
    startDate: Option[DateTime] = None,
    endDate: Option[DateTime] = None,
    created: Option[DateTime] = None,
    area: Option[Area]=None,
    methods: Option[String]=None,
    authors: Option[String]=None
)

