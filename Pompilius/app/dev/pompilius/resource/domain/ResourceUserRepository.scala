package dev.pompilius.resource.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.infrastructure.repositories.ResourceUserMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.{User, UserId}
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[ResourceUserMySqlRepository])
trait ResourceUserRepository {

  def findBy(resourceUser: ResourceUser): Future[Option[ResourceId]]

  def findByUserAndType(userId: UserId, resourceUserType: ResourceUserType, pag: Pagination): Future[List[ResourceId]]

  def findByResourceAndUser(resourceId: ResourceId, userId: UserId): Future[Option[ResourceUser]]

  def findByUserId(userId: UserId): Future[List[ResourceUser]]

  def save(resourceUser: ResourceUser): Future[Done]

  def findOwnerByResource(resourceId: ResourceId): Future[Option[User]]

  def deleteAllResourceByUserId(userId: UserId): Future[Done]

  def deleteRelation(resourceId: ResourceId, userId: UserId): Future[Done]
}
