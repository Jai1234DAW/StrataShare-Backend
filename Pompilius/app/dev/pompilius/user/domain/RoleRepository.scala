package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done


@ImplementedBy(classOf[RoleMySqlRepository])
trait RoleRepository {
  def findById(roleId:RoleId): Future[Option[Role]]
  def findByName(name: String): Future[Option[Role]]
  def find(name: Option[String], pag: Pagination): Future[List[Role]]
  def getAll: Future[List[Role]]
  def save(role: Role): Future[Done]
  def delete(roleId: RoleId): Future[Done]
}