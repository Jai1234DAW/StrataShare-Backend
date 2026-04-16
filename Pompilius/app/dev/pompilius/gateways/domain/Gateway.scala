package dev.pompilius.payment.domain.gateway

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait Gateway extends EnumEntry {
  def value: String
}

object Gateway extends Enum[Gateway] with PlayJsonEnum[Gateway] {

  val values: IndexedSeq[Gateway] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object STRIPE extends Gateway {
    override def value: String = "STRIPE"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object STRIPE_MOBILE extends Gateway {
    override def value: String = "STRIPE_MOBILE"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BARTER extends Gateway {
    override def value: String = "BARTER"
  }

  //Implementación Futura, se agregan más gateways a medida que se integren
  //  @SuppressWarnings(Array("ObjectNames"))
  //  case object PAYPAL extends Gateway {
  //    override def value: String = "PAYPAL"
  //  }

}
