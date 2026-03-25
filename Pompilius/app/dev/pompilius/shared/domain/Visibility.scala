package dev.pompilius.shared.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait Visibility extends EnumEntry {
  def value: String
}

object Visibility extends Enum[Visibility] with PlayJsonEnum[Visibility] {
  val values: IndexedSeq[Visibility] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object PUBLIC extends Visibility {
    override def value: String = "PUBLIC"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PRIVATE extends Visibility {
    override def value: String = "PRIVATE"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RESTRICTED extends Visibility {
    override def value: String = "RESTRICTED"
  }

}

