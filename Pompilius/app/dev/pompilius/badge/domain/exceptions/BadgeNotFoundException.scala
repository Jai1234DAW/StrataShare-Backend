package dev.pompilius.badge.domain.exceptions

import dev.pompilius.barter.domain.BarterId
import dev.pompilius.shared.domain.VerboseException

class BadgeNotFoundException(message: String = "Resource not found") extends VerboseException(message = message)

object BadgeNotFoundException {
  def apply(barterId: BarterId): BadgeNotFoundException = {
    new BadgeNotFoundException(s"Sample with id=${barterId.toString} not found")
  }
}