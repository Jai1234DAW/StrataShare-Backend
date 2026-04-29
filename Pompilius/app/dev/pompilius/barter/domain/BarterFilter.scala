package dev.pompilius.barter.domain

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.users.domain.UserId

case class BarterFilter(
    transactionId: Option[TransactionId] = None,
    offeredResourceId: Option[ResourceId] = None,
    userBuyer: Option[UserId] = None,
    userSeller: Option[UserId] = None
)
