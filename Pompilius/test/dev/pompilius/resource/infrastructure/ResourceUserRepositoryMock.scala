package dev.pompilius.resource.infrastructure

import dev.pompilius.resource.domain.{ResourceId, ResourceUser, ResourceUserRepository}
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait ResourceUserRepositoryMock extends MockitoSugar {

  protected val resourceUserRepository: ResourceUserRepository = mock[ResourceUserRepository]

  when(resourceUserRepository.save(any[ResourceUser]())).thenReturn(Future.successful(Done))
  when(resourceUserRepository.deleteRelation(any[ResourceId](), any[UserId]())).thenReturn(Future.successful(Done))
}

