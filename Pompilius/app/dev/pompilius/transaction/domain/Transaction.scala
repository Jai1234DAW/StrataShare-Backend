package dev.pompilius.transaction.domain

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime

case class Transaction(
    id: TransactionId,
    transactionType: TransactionType,
    transactionStatus: TransactionStatus,
    sellerId: UserId,
    buyerId: UserId,
    resourceId: ResourceId,
    fee: BigDecimal,
    created: DateTime,
    updated: DateTime,
    metadata: Option[String] = None,
    completedAt: Option[DateTime] = None,
    cancelledAt: Option[DateTime] = None
)
