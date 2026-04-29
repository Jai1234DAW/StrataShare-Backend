package dev.pompilius.resource.domain

import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class ResourceUser(
    resourceId: ResourceId,
    userId: UserId,
    resourceUserType: ResourceUserType,
    created: DateTime,
    updated: DateTime,
    deleted: Boolean = false
)