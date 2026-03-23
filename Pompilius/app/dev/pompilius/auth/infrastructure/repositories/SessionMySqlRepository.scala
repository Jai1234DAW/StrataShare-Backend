package dev.pompilius.auth.infrastructure.repositories

import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._
import dev.pompilius.auth.domain.{Session, SessionId, SessionRepository, SessionFilter}
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SessionMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends SessionRepository
    with SQLSyntaxSupport[Session] {

  override val tableName = "session"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(s: SyntaxProvider[Session])(rs: WrappedResultSet): Session =
    apply(s.resultName)(rs)
  def apply(s: ResultName[Session])(rs: WrappedResultSet): Session =
    Session(
      userId = UserId(rs.get[Long](s.userId)),
      id = SessionId(rs.get[Long](s.id)),
      deleted = rs.get(s.deleted),
      created = rs.get(s.created),
      address = rs.get(s.address),
      userAgent = rs.get(s.userAgent),
      country = rs.get[Option[String]](s.country).flatMap(Country.withNameInsensitiveOption),
      updatedAt = rs.get[Option[org.joda.time.DateTime]](s.updatedAt)
    )

  val s = this.syntax("s")

  override def findById(sessionId: SessionId): Future[Option[Session]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.eq(s.id, sessionId.id)
        }.map(apply(s.resultName)(_)).single()
      }
    }

  override def find(filter: SessionFilter, pag: Pagination): Future[List[Session]] =
    Future {

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(s.id)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(s.resultName)(_)).list()
      }
    }

  private def filterToSqlSyntax(filter: SessionFilter): Option[SQLSyntax] = {
    val filters = List(
      filter.id.map(id => sqls.eq(s.id, id.id)),
      filter.deleted.map(deleted => sqls.eq(s.deleted, deleted)),
      filter.userId.map(userId => sqls.eq(s.userId, userId.id))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def save(
      session: Session
  ): Future[Done] =
    Future {
      DB.localTx { implicit dBSession =>
        withSQL {
          insert
            .into(this)
            .namedValues(
              column.userId -> session.userId.id,
              column.id -> session.id.id,
              column.deleted -> session.deleted,
              column.created -> session.created,
              column.address -> session.address,
              column.userAgent -> session.userAgent,
              column.country -> session.country.map(_.toString),
                column.updatedAt -> session.updatedAt
            )
        }.update()
      }

      Done
    }

  override def closeAllSessions(userId: UserId, keep: Option[SessionId]): Future[Done] =
    Future {
      DB.localTx { implicit dBSession =>
        withSQL {
          update(this)
            .set(column.deleted -> true)
            .where
            .eq(column.userId, userId.id)
            .and(keep.map(id => sqls.ne(column.id, id.id)))
        }.update()
      }
      Done
    }

}
