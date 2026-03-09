package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done

import scala.concurrent.Future
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.user.infrastructure.repositories.UserRoleMySqlRepository

@ImplementedBy(classOf[UserRoleMySqlRepository])
trait UserRoleRepository {

  def getAllByUserId(userId: UserId): Future[List[UserRole]]

  def findBy(userId: UserId, role: Role): Future[Option[UserRole]]

  def find(filter: UserRoleFilter, pag: Pagination): Future[List[UserRole]]

  def save(userRole: UserRole): Future[Done]

  def setUserRoles(userId: UserId, roles: Set[Role]): Future[Done]
}