package dev.pompilius.payment.domain

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.resource.domain.Resource
import dev.pompilius.users.domain.User

//Mirar esto probablemente no lo necesite
case class PurchaseData(
    resource: Resource,
    seller: User,
    buyer: User,
    price: BigDecimal,
    fee: BigDecimal,
    totalAmount: BigDecimal,
    currency: String,
    gateway: Gateway
)

