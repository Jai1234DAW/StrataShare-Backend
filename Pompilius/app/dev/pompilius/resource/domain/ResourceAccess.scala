package dev.pompilius.resource.domain

import dev.pompilius.users.domain.UserId

case class ResourceAccess(
    resourceId: ResourceId,
    userId: UserId
)

