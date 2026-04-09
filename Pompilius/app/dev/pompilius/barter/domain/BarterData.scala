package dev.pompilius.barter.domain

import dev.pompilius.resource.domain.Resource
import dev.pompilius.users.domain.User

case class BarterData(
    requestedResource: Resource,
    offeredResource: Resource,
    buyer: User,
    seller: User
)
