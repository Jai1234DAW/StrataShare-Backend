package dev.pompilius.badge.infrastructure.repositories

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.badge.domain._
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BadgeMySqlRepository @Inject() ()(implicit dbExecutionContext: DbExecutionContext)
    extends BadgeRepository
    with SQLSyntaxSupport[Badge] {

  override val tableName = "badge"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(ba: SyntaxProvider[Badge])(rs: WrappedResultSet): Badge =
    apply(ba.resultName)(rs)

  def apply(ba: ResultName[Badge])(rs: WrappedResultSet): Badge =
    Badge(
      id = BadgeId(rs.get[Long](ba.id)),
      badgeType = BadgeType.withNameInsensitive(rs.get[String](ba.badgeType)),
      name = rs.get(ba.name),
      description = rs.get(ba.description),
      imageUrl = rs.get[Option[String]](ba.imageUrl),
      created = rs.get(ba.created)
    )

  private val ba = this.syntax("ba")

  override def findById(badgeId: BadgeId): Future[Option[Badge]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ba).where.eq(ba.id, badgeId.id)
        }.map(apply(ba.resultName)(_)).single()
      }
    }

  override def findByType(badgeType: BadgeType): Future[Option[Badge]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ba).where.eq(ba.badgeType, badgeType.value)
        }.map(apply(ba.resultName)(_)).single()
      }
    }

  override def findAll: Future[List[Badge]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ba).orderBy(ba.id).asc
        }.map(apply(ba.resultName)(_)).list()
      }
    }

  override def save(badge: Badge): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> badge.id.id,
          column.badgeType -> badge.badgeType.value,
          column.name -> badge.name,
          column.description -> badge.description,
          column.imageUrl -> badge.imageUrl,
          column.created -> badge.created
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

  override def delete(badgeId: BadgeId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.id, badgeId.id)
        }.update()
      }
      Done
    }
}

