package dev.pompilius.transaction.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait TransactionType extends EnumEntry {
  def value: String
}

object TransactionType extends Enum[TransactionType] with PlayJsonEnum[TransactionType] {

  val values: IndexedSeq[TransactionType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object PAYMENT extends TransactionType {
    override def value: String = "PAYMENT"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BARTER extends TransactionType {
    override def value: String = "BARTER"
  }

}
