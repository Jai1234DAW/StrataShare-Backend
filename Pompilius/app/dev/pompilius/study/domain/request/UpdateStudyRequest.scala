package dev.pompilius.study.domain.request

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.study.domain.Area
import org.joda.time.DateTime

case class UpdateStudyRequest(
    // Datos comunes (Resource)
    visibility: Option[Visibility] = None,
    localization: Option[String] = None,
    observations: Option[String] = None,
    summary: Option[String] = None,
    // Datos específicos (Study)
    name: Option[String] = None,
    startDate: Option[DateTime] = None,
    endDate: Option[DateTime] = None,
    description: Option[String] = None,
    coordinates: Option[String] = None,
    area: Option[Area] = None,
    methods: Option[String] = None,
    authors: Option[String] = None,
    section: Option[Boolean] = None,
    antecedents: Option[Boolean] = None,
    nameSection: Option[String] = None
)

