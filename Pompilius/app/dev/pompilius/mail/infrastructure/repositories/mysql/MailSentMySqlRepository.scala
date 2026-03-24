package dev.pompilius.mail.infrastructure.repositories.mysql

import dev.pompilius.mail.domain.{MailSent, MailSentFilter, MailSentId, MailSentRepository, MailType}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.domain.exceptions.UnprocessableException
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import java.sql.SQLIntegrityConstraintViolationException
import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

@Singleton
class MailSentMySqlRepository @Inject() (implicit ec: DbExecutionContext)
    extends MailSentRepository
    with SQLSyntaxSupport[MailSent] {

  override val tableName = "mail_sent"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(ms: SyntaxProvider[MailSent])(rs: WrappedResultSet): MailSent =
    apply(ms.resultName)(rs)
  def apply(ms: ResultName[MailSent])(rs: WrappedResultSet): MailSent =
    MailSent(
      id = MailSentId(rs.get[Long](ms.id)),
      mailType = MailType.withNameInsensitive(rs.get[String](ms.mailType)),
      address = rs.get(ms.address),
      sentAt = rs.get(ms.sentAt),
      userId = UserId(rs.get[Long](ms.id)),
      metadata = rs.get(ms.metadata)
    )

  private val ms = this.syntax("ms")

  override def findById(mailSentId: MailSentId): Future[Option[MailSent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ms).where.eq(ms.id, mailSentId.id)
        }.map(apply(ms.resultName)(_)).single()
      }
    }

  private def filterToSqlSyntax(filter: MailSentFilter): Option[SQLSyntax] = {

    val filters = List(
      filter.mailType.map(mailType => sqls.eq(ms.mailType, mailType.toString)),
      filter.address.map(address => sqls.like(ms.address, ScalikeUtil.normalizeSearch(address))),
      filter.userId.map(userId => sqls.eq(ms.userId, userId.id))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def find(filter: MailSentFilter, pag: Pagination): Future[List[MailSent]] =
    Future {

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ms)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(ms.id.desc)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(ms.resultName)(_)).list()
      }
    }

  override def save(mailSent: MailSent): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> mailSent.id.id,
          column.mailType -> mailSent.mailType.toString,
          column.address -> mailSent.address,
          column.sentAt -> mailSent.sentAt,
          column.userId -> mailSent.userId.id,
          column.metadata -> mailSent.metadata
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, values: _*))
        }.update()
      }
      Done
    }

  override def delete(mailSentId: MailSentId): Future[Done] =
    Future {
      try {
        DB.localTx { implicit session =>
          withSQL {
            deleteFrom(this).where.eq(column.id, mailSentId.id)
          }.update()
        }

        Done
      } catch {
        case _: SQLIntegrityConstraintViolationException =>
          throw new UnprocessableException("Cannot delete mailSent it is in use")
      }
    }
}
