package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.user.infrastructure.repositories.RoleMySqlRepository
import org.apache.pekko.Done
import scala.concurrent.Future


@ImplementedBy(classOf[RoleMySqlRepository])
trait RoleRepository {
  def findById(roleId:RoleId): Future[Option[Role]]
  def findByName(name: String): Future[Option[Role]]
  def find(name: Option[String], pag: Pagination): Future[List[Role]]
  def getAll: Future[List[Role]]
  def save(role: Role): Future[Done]
  def delete(roleId: RoleId): Future[Done]
}