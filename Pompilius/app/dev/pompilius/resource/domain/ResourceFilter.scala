package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class ResourceFilter(
    resourceType: Option[ResourceType] = None,
    name: Option[String]=None,
    ownerId: Option[UserId] = None,
    visibility: Option[Visibility] = None,
    createdFrom: Option[DateTime] = None,
    createdTo: Option[DateTime] = None,
    location: Option[String] = None
)

