package dev.pompilius.barter.domain

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.transaction.domain.TransactionId
import org.joda.time.DateTime

case class Barter(
    barterId: BarterId,
    transactionId: TransactionId,
    offeredResourceId: ResourceId,
)
