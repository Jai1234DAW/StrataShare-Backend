package dev.pompilius.resource.infrastructure

import dev.pompilius.resource.domain.{Resource, ResourceId, ResourceRepository}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait ResourceRepositoryMock extends MockitoSugar {

  protected val resourceRepository: ResourceRepository = mock[ResourceRepository]

  when(resourceRepository.save(any[Resource]())).thenReturn(Future.successful(Done))
  when(resourceRepository.delete(any[ResourceId]())).thenReturn(Future.successful(Done))
}

