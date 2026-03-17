package dev.pompilius.users.infrastructure.repositories

import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.{UserAttachment, UserAttachmentRepository, UserId, UserAttachmentFilter}
import org.apache.pekko.Done
import scalikejdbc._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil

@Singleton
class UserAttachmentMySqlRepository @Inject() (implicit ec: DbExecutionContext)
    extends UserAttachmentRepository
    with SQLSyntaxSupport[UserAttachment] {

  override val tableName = "users_attachment"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(ua: SyntaxProvider[UserAttachment])(rs: WrappedResultSet): UserAttachment =
    apply(ua.resultName)(rs)

  def apply(ua: ResultName[UserAttachment])(rs: WrappedResultSet): UserAttachment =
    UserAttachment(
      userId = UserId(rs.get[Long](ua.userId)),
      attachmentId = AttachmentId(rs.get[Long](ua.attachmentId))
    )

  private val ua = this.syntax("pa")

  override def findBy(userId: UserId, attachmentId: AttachmentId): Future[Option[UserAttachment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ua).where
            .eq(ua.userId, userId.id)
            .and
            .eq(ua.attachmentId, attachmentId.id)
        }.map(apply(ua.resultName)(_)).single()
      }
    }

  private def filterToSqlSyntax(filter: UserAttachmentFilter): Option[SQLSyntax] = {
    val filters = List(
      filter.userId.map(id => sqls.eq(ua.userId, id.id)),
      filter.attachmentId.map(id => sqls.eq(ua.attachmentId, id.id))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def find(filter: UserAttachmentFilter, pag: Pagination): Future[List[UserAttachment]] =
    Future {

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ua)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(ua.userId, ua.attachmentId)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(ua.resultName)(_)).list()
      }
    }

  override def save(userAttachment: UserAttachment): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.userId -> userAttachment.userId.id,
          column.attachmentId -> userAttachment.attachmentId.id
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(values: _*))
        }.update()
      }
      Done
    }

  //MIRAR SI ESTO ES NECESARIO???
  override def delete(userId: UserId, attachmentId: AttachmentId): Future[Done] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.userId, userId.id).and.eq(column.attachmentId, attachmentId.id)
        }.update()
      }
      Done
    }
  }

}
