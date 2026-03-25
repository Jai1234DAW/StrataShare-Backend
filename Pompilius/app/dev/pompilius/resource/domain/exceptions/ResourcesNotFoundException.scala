package dev.pompilius.resource.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.resource.domain.ResourceId

class ResourceNotFoundException(message: String = "Resource not found") extends VerboseException(message = message)

object ResourcesNotFoundException {
  def apply(resourceId: ResourceId): ResourceNotFoundException = {
    new ResourceNotFoundException(s"Sample with id=${resourceId.toString} not found")
  }
}
