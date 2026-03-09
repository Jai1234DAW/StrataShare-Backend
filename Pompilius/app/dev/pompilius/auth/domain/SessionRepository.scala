package dev.pompilius.auth.domain

import com.google.inject.ImplementedBy
import dev.pompilius.auth.infrastructure.repositories.SessionMySqlRepository
import dev.pompilius.user.domain.UserId
import org.apache.pekko.Done
import dev.pompilius.shared.domain.Pagination
import scala.concurrent.Future

@ImplementedBy(classOf[SessionMySqlRepository])
trait SessionRepository {
  def findById(sessionId: SessionId): Future[Option[Session]]
  def find(filter: SessionFilter, pag: Pagination): Future[List[Session]]
  def save(session: Session): Future[Done]
  def closeAllSessions(userId: UserId, Keep: Option[SessionId]): Future[Done]
}
