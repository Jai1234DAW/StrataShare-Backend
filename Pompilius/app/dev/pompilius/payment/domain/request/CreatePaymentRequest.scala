package dev.pompilius.payment.domain.request

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.RequestFingerprint

case class CreatePaymentRequest(
    resourceId: ResourceId,
    gateway: Gateway,
    buyerReference: Option[String],
    instrument: Option[String],
    //couponCode: Option[String],
    returnUrlParams: Option[Map[String, String]],
    extraInfo: Option[String],
)
