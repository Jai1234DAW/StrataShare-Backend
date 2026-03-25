package dev.pompilius.sample.domain.exceptions

import dev.pompilius.shared.domain.exceptions.NotFoundException


class SampleNotFoundException(message: String = "Sample not found") extends VerboseException(message = message)

object SampleNotFoundException {
  def apply(sampleId: SampleId): SampleNotFoundException = {
    new SampleNotFoundException(s"Sample with id=${sampleId.toString} not found")
  }
}
