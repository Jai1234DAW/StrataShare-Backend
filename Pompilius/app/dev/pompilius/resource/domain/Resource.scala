package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class Resource(
    id: ResourceId,
    resourceType: ResourceType,
    ownerId: UserId,
    visibility: Visibility,
    created: DateTime,
    updated: DateTime,
    observations: Option[String] = None,
    summary: Option[String] = None,
    localization:String
)

