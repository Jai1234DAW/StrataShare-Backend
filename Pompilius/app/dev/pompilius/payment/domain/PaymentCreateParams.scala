package dev.pompilius.payment.domain

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.resource.domain.Resource
import dev.pompilius.shared.domain.RequestFingerprint
import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.users.domain.User

case class PaymentCreateParams(
    id: PaymentId,
    transactionId: TransactionId,
    gateway: Gateway,
    instrument: Option[String],
    buyer: User,
    seller: User,
    resource: Resource,
    price: BigDecimal,
    amount: BigDecimal,
    surcharge: BigDecimal,
    //couponCode: Option[String],
    discount: BigDecimal,
    //automaticTax: Boolean,
    fingerprint: Option[RequestFingerprint],
    returnUrlParams: Option[Map[String, String]],
    extraInfo: Option[String]
)
