package dev.pompilius.user.domain

import com.google.inject.ImplementedBy


@ImplementedBy(classOf[RoleMySqlRepository])
trait RoleRepository {
  def findById(roleId:RoleId): Future[Option[Role]]
}