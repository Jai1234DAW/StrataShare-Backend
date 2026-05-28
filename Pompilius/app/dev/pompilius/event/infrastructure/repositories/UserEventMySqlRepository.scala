package dev.pompilius.event.infrastructure.repositories

import dev.pompilius.event.domain.{EventU, UserEvent, UserEventId, UserEventRepository}
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UserEventMySqlRepository @Inject() ()(implicit dbExecutionContext: DbExecutionContext)
    extends UserEventRepository
    with SQLSyntaxSupport[UserEvent] {

  override val tableName = "users_events"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(ue: SyntaxProvider[UserEvent])(rs: WrappedResultSet): UserEvent =
    apply(ue.resultName)(rs)

  def apply(ue: ResultName[UserEvent])(rs: WrappedResultSet): UserEvent =
    UserEvent(
      id = UserEventId(rs.get[Long](ue.id)),
      userId = UserId(rs.get[Long](ue.userId)),
      event = EventU.withName(rs.get[String](ue.event)),
      created = rs.get(ue.created)
    )

  private val ue = this.syntax("ue")

  override def getAllByUserId(userId: UserId): Future[List[UserEvent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ue).where
            .eq(ue.userId, userId.id)
            .orderBy(ue.created)
            .desc
        }.map(apply(ue.resultName)(_)).list()
      }
    }

  override def findBy(userId: UserId, event: EventU): Future[Option[UserEvent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ue).where
            .eq(ue.userId, userId.id)
            .and
            .eq(ue.event, event.value)
            .limit(1)
        }.map(apply(ue.resultName)(_)).single()
      }
    }

  override def save(userEvent: UserEvent): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          insert
            .into(this)
            .namedValues(
              column.id -> userEvent.id.id,
              column.userId -> userEvent.userId.id,
              column.event -> userEvent.event.value,
              column.created -> userEvent.created
            )
        }.update()
      }
      Done
    }

  override def countByUserAndEvent(userId: UserId, event: EventU): Future[Int] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          select(sqls.count)
            .from(this as ue)
            .where
            .eq(ue.userId, userId.id)
            .and
            .eq(ue.event, event.value)
        }.map(rs => rs.int(1)).single().getOrElse(0)
      }
    }

  override def countAllEventsByUserId(userId: UserId): Future[Int] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          select(sqls.count)
            .from(this as ue)
            .where
            .eq(ue.userId, userId.id)
        }.map(rs => rs.int(1)).single().getOrElse(0)
      }
    }
}
