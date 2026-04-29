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
    //Esta es la comisión de mi plataforma, pero debería ser solo para payments, para barter no se cobra comisión
    fee: Option[BigDecimal],
    created: DateTime,
    updated: DateTime,
    metadata: Option[String] = None,
    completedSuccessfullyAt: Option[DateTime] = None,
    cancelledRejectedAt: Option[DateTime] = None
)
