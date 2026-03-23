package dev.pompilius.auth.application

import com.google.inject.ImplementedBy
import dev.pompilius.auth.domain.exceptions.InvalidPasswordOrUsernameException
import dev.pompilius.auth.domain.request.LoginAsRequest
import dev.pompilius.auth.domain.{Session, SessionId, SessionRepository}
import dev.pompilius.shared.domain.{Clock, Configuration, RequestFingerprint}
import dev.pompilius.users.domain.{User, UserRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SessionCreatorImpl])
trait SessionCreator {
  def create(user: User, requestFingerprint: RequestFingerprint): Future[Session]
  def loginAs(loginAsRequest: LoginAsRequest, requestFingerprint: RequestFingerprint): Future[Session]
}

@Singleton
class SessionCreatorImpl @Inject() (
    sessionRepository: SessionRepository,
    userRepository: UserRepository,
    clock: Clock,
    configuration: Configuration
)(implicit ec: ExecutionContext)
    extends SessionCreator {

  override def create(user: User, requestFingerprint: RequestFingerprint): Future[Session] = {
    val session = Session(
      id = SessionId.gen(configuration.nodeId),
      userId = user.id,
      deleted = false,
      created = clock.now.withMillisOfSecond(0),
      address = requestFingerprint.remoteAddress,
      userAgent = requestFingerprint.userAgent,
      country = requestFingerprint.country
    )

    sessionRepository
      .save(session)
      .map(_ => session)
      .map(_ => session)
  }

  override def loginAs(
      loginAsRequest: LoginAsRequest,
      requestFingerprint: RequestFingerprint
  ): Future[Session] = {
    userRepository.findByUsername(loginAsRequest.username).flatMap {
      case Some(user) =>
        create(user, requestFingerprint)
      case _ =>
        throw new InvalidPasswordOrUsernameException()
    }
  }

}
