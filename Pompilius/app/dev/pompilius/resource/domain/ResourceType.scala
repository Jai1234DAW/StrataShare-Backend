package dev.pompilius.resource.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait ResourceType extends EnumEntry {
  def value: String
}

object ResourceType extends Enum[ResourceType] with PlayJsonEnum[ResourceType] {

  val values: IndexedSeq[ResourceType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object STUDY extends ResourceType {
    override def value: String = "STUDY"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SAMPLE extends ResourceType {
    override def value: String = "SAMPLE"
  }

}

