package dev.pompilius.barter.domain.exception

import dev.pompilius.barter.domain.BarterId
import dev.pompilius.shared.domain.VerboseException

class BarterNotFoundException(message: String = "Resource not found") extends VerboseException(message = message)

object BarterNotFoundException {
  def apply(barterId: BarterId): BarterNotFoundException = {
    new BarterNotFoundException(s"Sample with id=${barterId.toString} not found")
  }
}