package dev.pompilius.event.domain

import com.google.inject.ImplementedBy
import dev.pompilius.event.infrastructure.repositories.UserEventMySqlRepository
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserEventMySqlRepository])
trait UserEventRepository {

  def getAllByUserId(userId: UserId): Future[List[UserEvent]]

  def findBy(userId: UserId, event: EventU): Future[Option[UserEvent]]

  def save(userEvent: UserEvent): Future[Done]

  def countByUserAndEvent(userId: UserId, event: EventU): Future[Int]

  def countAllEventsByUserId(userId: UserId): Future[Int]

}