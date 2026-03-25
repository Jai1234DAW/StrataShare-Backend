package dev.pompilius.resource.domain.study

import dev.pompilius.resource.domain.ResourceId
import org.joda.time.DateTime


case class Study(
    id: StudyId,
    resourceId: ResourceId,
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
