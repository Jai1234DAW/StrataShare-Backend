package dev.pompilius.auth.application

import com.google.inject.ImplementedBy
import dev.pompilius.auth.domain.{Session, SessionId, SessionRepository}
import dev.pompilius.auth.domain.request.LoginRequest
import dev.pompilius.shared.domain.{Clock, Configuration, RequestFingerprint}
import dev.pompilius.users.domain.UserRepository
import dev.pompilius.auth.domain.exceptions.InvalidPasswordOrUsernameException


import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[LoginValidatorImpl])
trait LoginValidator {
  def validateLoginAndPassword(
      loginPassword: LoginRequest,
      requestFingerprint: RequestFingerprint
  ): Future[Session]
}

@Singleton
class LoginValidatorImpl @Inject() (
    sessionRepository: SessionRepository,
    userRepository: UserRepository,
    clock: Clock,
    configuration: Configuration,
)(implicit ec: ExecutionContext)
    extends LoginValidator {

  // Valido la contraseña y el username, si son correctos creo la sesión y la guardo en la base de datos, sino lanzo una excepción
  override def validateLoginAndPassword(
      loginRequest: LoginRequest,
      requestFingerprint: RequestFingerprint
  ): Future[Session] = {
    userRepository.findByUsername(loginRequest.username).flatMap {
      case Some(user) if user.passwordHash.nonEmpty && loginRequest.password.check(user.passwordHash) =>
        val session = Session(
          id=SessionId.gen(configuration.nodeId),
          userId = user.id,
          deleted = false,
          created = clock.now.withMillisOfSecond(0),
          address = requestFingerprint.remoteAddress,
          userAgent = requestFingerprint.userAgent,
          country = requestFingerprint.country
        )

        sessionRepository.save(session).map(_ => session)

      case _ =>
        throw new InvalidPasswordOrUsernameException()
    }
  }
}
