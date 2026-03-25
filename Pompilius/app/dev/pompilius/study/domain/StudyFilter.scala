package dev.pompilius.studies.domain

import org.joda.time.DateTime

case class StudyFilter(
    name: Option[String] = None,
    visibility: Option[String] = None,
    localization: Option[String] = None,
    startDateFrom: Option[DateTime] = None,
    startDateTo: Option[DateTime] = None,
    endDateFrom: Option[DateTime] = None,
    endDateTo: Option[DateTime] = None,
    area: Option[Area]=None,
    methods: Option[String]=None
)

