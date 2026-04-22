package dev.pompilius.event.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait EventU extends EnumEntry {
  def value: String
}

object EventU extends Enum[EventU] with PlayJsonEnum[EventU] {

  val values: IndexedSeq[EventU] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object PURCHASE_COMPLETED extends EventU {
    override def value: String = "PURCHASE_COMPLETED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BARTER_COMPLETED extends EventU {
    override def value: String = "BARTER_COMPLETED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SAMPLE_UPLOADED extends EventU {
    override def value: String = "SAMPLE_UPLOADED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object STUDY_UPLOADED extends EventU {
    override def value: String = "STUDY_UPLOADED"
  }
}

