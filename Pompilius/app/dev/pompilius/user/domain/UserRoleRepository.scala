package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserRoleMySqlRepository])
trait RolePermissionRepository {

  def getAllByUserId(userId: UserId): Future[List[UserRole]]

  def setUserRoles(UserId: UserId,roles: List[Role]): Future[Done]

}
