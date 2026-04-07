package dev.pompilius.resource.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait ResourceAccessLevel extends EnumEntry {
  def value: String
}

object ResourceAccessLevel extends Enum[ResourceAccessLevel] with PlayJsonEnum[ResourceAccessLevel] {
  val values: IndexedSeq[ResourceAccessLevel] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object FULL_ACCESS extends ResourceAccessLevel {
    override def value: String = "FULL_ACCESS"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PREVIEW_ONLY extends ResourceAccessLevel {
    override def value: String = "PREVIEW_ONLY"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NO_ACCESS extends ResourceAccessLevel {
    override def value: String = "NO_ACCESS"
  }

}


