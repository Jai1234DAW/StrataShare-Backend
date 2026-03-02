package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import dev.pompilius.user.infrastructure.repositories.RolePermissionMySqlRepository
import org.apache.pekko.Done
import scala.concurrent.Future

@ImplementedBy(classOf[RolePermissionMySqlRepository])
trait RolePermissionRepository {

  def getAllByRoleId(roleId: RoleId): Future[List[RolePermission]]
  def setRolePermissions(roleId: RoleId, permissions: List[Permission]): Future[Done]
}
