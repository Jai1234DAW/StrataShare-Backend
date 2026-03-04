package dev.pompilius.auth.domain

import com.google.inject.ImplementedBy
import dev.pompilius.user.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[SessionMySqlRepository])
trait SessionRepository {
  def findSession(userId: UserId, token: SessionToken): Future[Option[Session]]
  def save(session: Session): Future[Done]
  def closeAllSessions(userId: UserId, Keep: Option[SessionToken]): Future[Done]
}
