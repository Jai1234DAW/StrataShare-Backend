package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

case class Resource (
   id: ResourceId,
   resourceType: ResourceType,
  visibility: Visibility,
  created: DateTime,
  updated: DateTime,
  localization: String,
  observations: Option[String],
  summary: Option[String]
  )
