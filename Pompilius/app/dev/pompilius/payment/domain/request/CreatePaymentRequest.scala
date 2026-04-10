package dev.pompilius.payment.domain.request

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.transaction.domain.{TransactionStatus, TransactionType}
import dev.pompilius.users.domain.UserId

case class CreatePaymentRequest(
    transactionType: TransactionType,
    transactionStatus: TransactionStatus,
    sellerId: UserId,
    buyerId: UserId,
    resourceId: ResourceId,
    metadata: Option[String] = None
)
