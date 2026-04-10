package dev.pompilius.payment.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait PaymentIntentStatus extends EnumEntry {
  def value: String
}

object PaymentIntentStatus extends Enum[PaymentIntentStatus] with PlayJsonEnum[PaymentIntentStatus] {

  val values: IndexedSeq[PaymentIntentStatus] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object REQUIRES_PAYMENT_METHOD extends PaymentIntentStatus {
    override def value: String = "requires_payment_method"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object REQUIRES_CONFIRMATION extends PaymentIntentStatus {
    override def value: String = "requires_confirmation"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object REQUIRES_ACTION extends PaymentIntentStatus {
    override def value: String = "requires_action"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PROCESSING extends PaymentIntentStatus {
    override def value: String = "processing"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SUCCEEDED extends PaymentIntentStatus {
    override def value: String = "succeeded"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CANCELED extends PaymentIntentStatus {
    override def value: String = "canceled"
  }

}
