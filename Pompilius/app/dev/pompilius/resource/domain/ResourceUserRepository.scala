package dev.pompilius.resource.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.infrastructure.repositories.ResourceUserMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[ResourceUserMySqlRepository])
trait ResourceUserRepository {

  def getAllByResourceId(resourceId: ResourceId): Future[List[ResourceUser]]

  def getAllByUserId(userId: UserId): Future[List[ResourceUser]]

  def findBy(resourceId: ResourceId, userId: UserId): Future[Option[ResourceUser]]

  def find(filter: ResourceUserFilter, pag: Pagination): Future[List[ResourceUser]]

  def save(resourceUser: ResourceUser): Future[Done]
}

