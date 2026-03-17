package dev.pompilius.users.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait Role extends EnumEntry {
  def description: String
  def level:Int
}

object Role extends Enum[Role] with PlayJsonEnum[Role] {

  val values: IndexedSeq[Role] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object ADMIN extends Role {
    val description = "Administrator"
    val level = 1
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PROFESSIONAL extends Role {
    val description = "Researcher"
    val level = 2
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object STUDENT extends Role {
    val description = "Support"
    val level = 3
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AMATEUR extends Role {
    val description = "Researcher"
    val level = 4
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SUPPORT extends Role {
    val description = "Support"
    val level = 99
  }

}

