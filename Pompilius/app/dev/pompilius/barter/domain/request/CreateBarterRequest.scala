package dev.pompilius.barter.domain.request

import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.transaction.domain.{TransactionStatus, TransactionType}
import dev.pompilius.users.domain.UserId

case class CreateBarterRequest(
    resourceId: ResourceId,
    //Campos Específicos para Barter
    offeredResourceId: ResourceId
)
