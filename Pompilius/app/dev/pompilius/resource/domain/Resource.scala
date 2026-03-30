package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.resource.domain.study.Area
import org.joda.time.DateTime

sealed trait Resource {
  val id: ResourceId
  val resourceType: ResourceType
  val deleted: Boolean
  val visibility: Visibility
  val created: DateTime
  val updated: DateTime
  val localization: String
  val observations: Option[String]
  val summary: Option[String]
}

case class Sample(
    id: ResourceId,
    resourceType: ResourceType = ResourceType.SAMPLE,
    deleted: Boolean = false,
    visibility: Visibility,
    created: DateTime,
    updated: DateTime,
    localization: String,
    observations: Option[String] = None,
    summary: Option[String] = None,
    // Campos específicos de Sample
    name: String,
    minerals: Option[String] = None,
    collectionMethods: Option[String] = None,
    isFresh: Boolean = false,
    sampleType: Option[String] = None,
    materialsUsed: Option[String] = None,
    rockType: Option[String] = None,
    geologicalProcesses: Option[String] = None
) extends Resource

case class Study(
    id: ResourceId,
    resourceType: ResourceType = ResourceType.STUDY,
    deleted: Boolean = false,
    visibility: Visibility,
    created: DateTime,
    updated: DateTime,
    localization: String,
    observations: Option[String] = None,
    summary: Option[String] = None,
    // Campos específicos de Study
    name: String,
    startDate: DateTime,
    endDate: Option[DateTime] = None,
    description: String,
    coordinates: String,
    area: Area,
    methods: String,
    authors: String,
    section: Boolean = false,
    antecedents: Boolean = true,
    nameSection: Option[String] = None
) extends Resource
