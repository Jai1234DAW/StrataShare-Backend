package dev.pompilius.mail.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait MailType extends EnumEntry

object MailType extends Enum[MailType] with PlayJsonEnum[MailType] {

  val values: IndexedSeq[MailType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object WELCOME extends MailType
}

