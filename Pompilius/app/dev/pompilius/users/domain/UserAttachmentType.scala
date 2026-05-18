package dev.pompilius.users.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

  sealed trait UserAttachmentType extends EnumEntry {
    def value: String
  }

  object UserAttachmentType extends Enum[UserAttachmentType] with PlayJsonEnum[UserAttachmentType] {
    val values: IndexedSeq[UserAttachmentType] = findValues

    @SuppressWarnings(Array("ObjectNames"))
    case object AVATAR extends UserAttachmentType {
      override def value: String = "AVATAR"
    }

    @SuppressWarnings(Array("ObjectNames"))
    case object COVER_PHOTO extends UserAttachmentType {
      override def value: String = "COVER_PHOTO"
    }

    @SuppressWarnings(Array("ObjectNames"))
    case object DOCUMENT extends UserAttachmentType {
      override def value: String = "DOCUMENT"
    }

    @SuppressWarnings(Array("ObjectNames"))
    case object OTHER extends UserAttachmentType {
      override def value: String = "OTHER"
    }

}
