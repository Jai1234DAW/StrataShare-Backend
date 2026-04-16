package dev.pompilius.customer.domain

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.users.domain.UserId

case class Customer(userId: UserId, gateway: Gateway, gatewayCustomerId: String, metadata: Option[String])
