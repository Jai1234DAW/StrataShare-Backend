package dev.pompilius.studies.domain

import dev.pompilius.attachment.domain.AttachmentId
import org.joda.time.DateTime

case class Study(
    id: StudyId,
    name: String,
    visibility: String,
    localization: String,
    startDate: DateTime,
    endDate: Option[DateTime],
    description: String,
    coordinates: String,
    observations: Option[String],
    summary: Option[String],
    created: DateTime,
    updated: DateTime,
    area:String,
    methods:String,
    authors:String,
    antecedent: Boolean,
    section: Boolean,
    NameSection: Option[String] =None,
attachments: List[AttachmentId] = List(),
    samples: List[String] = List() // Aquí irán los IDs de las muestras cuando se cree el módulo
)

