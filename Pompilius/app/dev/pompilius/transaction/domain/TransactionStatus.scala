package dev.pompilius.transaction.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait TransactionStatus extends EnumEntry {
  def value: String
}

object TransactionStatus extends Enum[TransactionStatus] with PlayJsonEnum[TransactionStatus] {

  val values: IndexedSeq[TransactionStatus] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object PENDING extends TransactionStatus {
    override def value: String = "PENDING"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object COMPLETED extends TransactionStatus {
    override def value: String = "COMPLETED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FAILED extends TransactionStatus {
    override def value: String = "FAILED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CANCELED extends TransactionStatus {
    override def value: String = "CANCELLED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object REJECTED extends TransactionStatus {
    override def value: String = "REJECTED"
  }

}

