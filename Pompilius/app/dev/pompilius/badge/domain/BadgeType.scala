package dev.pompilius.badge.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait BadgeType extends EnumEntry {
  def value: String
}

object BadgeType extends Enum[BadgeType] with PlayJsonEnum[BadgeType] {

  val values: IndexedSeq[BadgeType] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object SEDIMENT_COLLECTOR extends BadgeType {
    override def value: String = "SEDIMENT_COLLECTOR"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MINERAL_PROSPECTOR extends BadgeType {
    override def value: String = "MINERAL_PROSPECTOR"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CRYSTAL_SEEKER extends BadgeType {
    override def value: String = "CRYSTAL_SEEKER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DIAMOND_EXPLORER extends BadgeType {
    override def value: String = "DIAMOND_EXPLORER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FOSSIL_TRADER extends BadgeType {
    override def value: String = "FOSSIL_TRADER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ROCK_EXCHANGER extends BadgeType {
    override def value: String = "ROCK_EXCHANGER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEMSTONE_SWAPPER extends BadgeType {
    override def value: String = "GEMSTONE_SWAPPER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEODE_MASTER extends BadgeType {
    override def value: String = "GEODE_MASTER"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object STRATA_CONTRIBUTOR extends BadgeType {
    override def value: String = "STRATA_CONTRIBUTOR"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEOLOGICAL_LEGEND extends BadgeType {
    override def value: String = "GEOLOGICAL_LEGEND"
  }
}


