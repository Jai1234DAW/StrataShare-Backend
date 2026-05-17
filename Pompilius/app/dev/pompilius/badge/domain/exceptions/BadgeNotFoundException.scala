package dev.pompilius.badge.domain.exceptions


import dev.pompilius.badge.domain.BadgeId
import dev.pompilius.shared.domain.VerboseException

class BadgeNotFoundException(message: String = "Badge not found") extends VerboseException(message = message)

object BadgeNotFoundException {
  def apply(badgeId: BadgeId): BadgeNotFoundException = {
    new BadgeNotFoundException(s"Badge with id=${badgeId.toString} not found")
  }
}