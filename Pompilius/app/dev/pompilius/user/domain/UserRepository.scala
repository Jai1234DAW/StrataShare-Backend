package dev.pompilius.user.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.user.infrastructure.repositories.UserMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserMySqlRepository])
trait UserRepository {

  def getById(userId: UserId): Future[User]

  def findById(userId: UserId): Future[Option[User]]

  def findByUsername(username: String): Future[Option[User]]

  def findByEmail(email: String): Future[Option[User]]

  def find(filter: UserFilter, pag: Pagination): Future[List[User]]

  def save(user: User): Future[Done]

}

