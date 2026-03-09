package dev.pompilius.auth.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.user.domain.UserId

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SessionValidatorImpl])
trait SessionValidator {
  def validateSession(
      sessionId: SessionId,
      userId: UserId
  ): Future[Option[Session]]
}

@Singleton
class SessionValidatorImpl @Inject() (
    sessionRepository: SessionRepository,
    configuration: Configuration,
    clock: Clock
)(implicit val ec: ExecutionContext)
    extends SessionValidator {

  override def validateSession(
                                sessionId: SessionId,
                                userId: UserId,
                              ): Future[Option[Session]] = {
    sessionRepository.findSession(userId = userId, sessionId = sessionId).map {
      case Some(session) if session.deleted =>
        None
      case Some(session)
        if session.created.plusSeconds(configuration.session.maxAge.toSeconds.toInt).isBefore(clock.now) =>
        None
      case session =>
        session
    }
  }

}
