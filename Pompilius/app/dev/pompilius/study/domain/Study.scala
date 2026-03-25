package dev.pompilius.study.domain

import org.joda.time.DateTime

case class Study(
    id: StudyId,
    name: String,
    visibility: Visibility,
    localization: String,
    startDate: DateTime,
    endDate: Option[DateTime],
    description: String,
    coordinates: String,
    observations: Option[String],
    summary: Option[String],
    created: DateTime,
    updated: DateTime,
    area: Area,
    methods: String,
    authors: String,
    antecedent: Boolean,
    section: Boolean,
    nameSection: Option[String] = None
)

