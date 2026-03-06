package dev.pompilius.auth.infrastructure.repositories

import scalikejdbc._
import dev.pompilius.auth.domain.{Session, SessionRepository, SessionToken}

import javax.inject.{Inject, Singleton}
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.user.domain.UserId
import org.apache.pekko.Done
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import scala.concurrent.Future

class SessionMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends SessionRepository
    with SQLSyntaxSupport[Session] {
  override val tableName="session"

    override def findSession(userId: UserId, token: SessionToken): Future[Option[Session]]

    override def save(session: Session): Future[Done] = {

      override closeAllSessions()
    }


}
