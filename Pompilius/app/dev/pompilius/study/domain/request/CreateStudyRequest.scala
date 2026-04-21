package dev.pompilius.study.domain.request

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.study.domain.Area
import org.joda.time.DateTime

case class CreateStudyRequest(
    // Datos comunes (Resource)
    visibility: Visibility,
    localization: String,
    observations: Option[String],
    summary: Option[String],
    // Datos específicos (Study)
    name: String,
    startDate: DateTime,
    endDate: Option[DateTime],
    description: String,
    coordinates: String,
    area: Area,
    methods: String,
    authors: String,
    section: Boolean,
    antecedents: Boolean,
    nameSection: Option[String]
)
