package dev.pompilius.transaction.domain

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.users.domain.UserId

case class TransactionFilter(
    buyerId: Option[UserId] = None,
    sellerId: Option[UserId] = None,
    resourceId: Option[ResourceId] = None,
    transactionType: Option[TransactionType] = None, // PAYMENT o BARTER
    transactionStatus: Option[TransactionStatus] = None,
)
