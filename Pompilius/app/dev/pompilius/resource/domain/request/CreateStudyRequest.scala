package dev.pompilius.resource.domain.request

import dev.pompilius.resource.domain.study.Area
import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

case class CreateStudyRequest(
    visibility: Visibility,
    localization: String,
    observations: Option[String],
    summary: Option[String],
    name: String,
    startDate: DateTime,
    endDate: Option[DateTime],
    description: String,
    coordinates: String,
    area: Area,
    methods: String,
    authors: String,
    section: Boolean,
    antecedents: Boolean = true,
    nameSection: Option[String] = None
)

