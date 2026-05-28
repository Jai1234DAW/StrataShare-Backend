package dev.pompilius.report.domain

import enumeratum.{EnumEntry, PlayJsonEnum, Enum}

sealed trait DataType extends EnumEntry

object DataType extends Enum[DataType] with PlayJsonEnum[DataType] {
  val values: IndexedSeq[DataType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object INT extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object LONG extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object DATETIME extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object STRING extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object BOOLEAN extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object DOUBLE extends DataType

  @SuppressWarnings(Array("ObjectNames"))
  case object BIGDECIMAL extends DataType
}
