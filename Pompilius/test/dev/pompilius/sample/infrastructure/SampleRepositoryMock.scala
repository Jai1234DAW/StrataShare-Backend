package dev.pompilius.sample.infrastructure

import dev.pompilius.sample.domain.{Sample, SampleId, SampleRepository}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait SampleRepositoryMock extends MockitoSugar {

  protected val sampleRepository: SampleRepository = mock[SampleRepository]

  when(sampleRepository.save(any[Sample]())).thenReturn(Future.successful(Done))


  protected def sampleRepositoryShouldFind(sample: Option[Sample]): Unit = {
    when(sampleRepository.findById(any[SampleId])).thenReturn(Future.successful(sample))
  }

}
