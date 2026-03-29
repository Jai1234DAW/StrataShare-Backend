package dev.pompilius.resource.domain

import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class ResourceUserFilter(
    resourceId: Option[ResourceId] = None,
    userId: Option[UserId] = None,
    resourceUserType: Option[ResourceUserType] = None,
    date: Option[DateTime] = None
)
