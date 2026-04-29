package dev.pompilius.resource.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait ResourceUserType extends EnumEntry {
  def value: String
}

object ResourceUserType extends Enum[ResourceUserType] with PlayJsonEnum[ResourceUserType] {
  val values: IndexedSeq[ResourceUserType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object OWNER extends ResourceUserType {
    override def value: String = "OWNER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PURCHASED extends ResourceUserType {
    override def value: String = "PURCHASED"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ACCEPTED_AS_PAYMENT extends ResourceUserType {
    override def value: String = "ACCEPTED_AS_PAYMENT"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BARTERED extends ResourceUserType {
    override def value: String = "BARTERED"
  }
}

