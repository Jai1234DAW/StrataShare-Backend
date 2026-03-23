package dev.pompilius.mail.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait AttachmentDisposition extends EnumEntry

object AttachmentDisposition extends Enum[AttachmentDisposition] with PlayJsonEnum[AttachmentDisposition] {

  val values: IndexedSeq[AttachmentDisposition] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object INLINE extends AttachmentDisposition

  @SuppressWarnings(Array("ObjectNames"))
  case object ATTACHMENT extends AttachmentDisposition
}