package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{ResourceId, ResourceUser, ResourceUserFilter, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

@Singleton
class ResourceUserMySqlRepository @Inject()(implicit ec: DbExecutionContext)
    extends ResourceUserRepository
    with SQLSyntaxSupport[ResourceUser] {

  override val tableName = "resource_user"

  def apply(ru: SyntaxProvider[ResourceUser])(rs: WrappedResultSet): ResourceUser =
    apply(ru.resultName)(rs)

  def apply(ru: ResultName[ResourceUser])(rs: WrappedResultSet): ResourceUser =
    ResourceUser(
      resourceId = ResourceId(rs.get[Long](ru.resourceId)),
      userId = UserId(rs.get[Long](ru.userId)),
      resourceUserType = ResourceUserType.withNameInsensitive(rs.get[String](ru.resourceUserType)),
      created = rs.get(ru.grantedAt)
    )

  private val ru = this.syntax("ru")

  override def getAllByResourceId(resourceId: ResourceId): Future[List[ResourceUser]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where.eq(ru.resourceId, resourceId.id)
        }.map(apply(ru.resultName)(_)).list()
      }
    }

  override def getAllByUserId(userId: UserId): Future[List[ResourceUser]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where.eq(ru.userId, userId.id)
        }.map(apply(ru.resultName)(_)).list()
      }
    }

  override def findBy(resourceId: ResourceId, userId: UserId): Future[Option[ResourceUser]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru)
            .where.eq(ru.resourceId, resourceId.id)
            .and.eq(ru.userId, userId.id)
        }.map(apply(ru.resultName)(_)).single()
      }
    }

  override def find(filter: ResourceUserFilter, pag: Pagination): Future[List[ResourceUser]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(ru.resourceId, ru.userId)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(ru.resultName)(_)).list()
      }
    }

  private def filterToSqlSyntax(filter: ResourceUserFilter): Option[scalikejdbc.SQLSyntax] = {

    val filters = List(
      filter.resourceId.map(id => sqls.eq(ru.patentId, id.id)),
      filter.userId.map(id => sqls.eq(ru.personId, id.id))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def save(resourceUser: ResourceUser): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.resourceId -> resourceUser.resourceId.id,
          column.userId -> resourceUser.userId.id,
          column.resourceUserType -> resourceUser.resourceUserType.toString,
          column.created -> resourceUser.created,
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
        }.update()
      }
      Done
    }
}

