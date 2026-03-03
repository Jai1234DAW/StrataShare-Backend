package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import dev.pompilius.user.infrastructure.repositories.UserRoleMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserRoleMySqlRepository])
trait UserRoleRepository {

  def getAllByUserId(userId: UserId): Future[List[UserRole]]

  def setUserRoles(userId: UserId, roles: List[RoleId]): Future[Done]

}
