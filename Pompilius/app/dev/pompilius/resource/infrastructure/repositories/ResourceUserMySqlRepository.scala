package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.resource.domain.{ResourceId, ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.{User, UserId}
import dev.pompilius.users.infrastructure.repositories.UserMySqlRepository
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ResourceUserMySqlRepository @Inject() (
    userMySqlRepository: UserMySqlRepository
)(implicit ec: DbExecutionContext)
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
      created = rs.get(ru.created),
      deleted = rs.get[Boolean](ru.deleted)
    )

  private val ru = this.syntax("ru")

  override def findByUserId(userId: UserId): Future[List[ResourceUser]] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where.eq(ru.userId, userId.id)
        }.map(apply(ru.resultName)(_)).list()
      }
    }
  }

  override def save(resourceUser: ResourceUser): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.resourceId -> resourceUser.resourceId.id,
          column.userId -> resourceUser.userId.id,
          column.resourceUserType -> resourceUser.resourceUserType.toString,
          column.created -> resourceUser.created,
          column.deleted -> resourceUser.deleted
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

  override def findBy(resourceUser: ResourceUser): Future[Option[ResourceId]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where
            .eq(ru.resourceId, resourceUser.resourceId.id)
            .and
            .eq(ru.userId, resourceUser.userId.id)
        }.map(rs => ResourceId(rs.get[Long](ru.resultName.resourceId))).single()
      }
    }

  override def findByUserAndType(
      userId: UserId,
      resourceUserType: ResourceUserType,
      pag: Pagination
  ): Future[List[ResourceId]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where
            .eq(ru.userId, userId.id)
            .and
            .eq(ru.resourceUserType, resourceUserType.toString)
            .orderBy(ru.created)
            .desc
            .append(ScalikeUtil.pag(pag))
        }.map(rs => ResourceId(rs.get[Long](ru.resultName.resourceId))).list()
      }
    }

  override def findByResourceAndUser(resourceId: ResourceId, userId: UserId): Future[Option[ResourceUser]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ru).where.eq(ru.resourceId, resourceId.id).and.eq(ru.userId, userId.id)
        }.map(apply(ru.resultName)(_)).single()
      }
    }

  override def findOwnerByResource(resourceId: ResourceId): Future[Option[User]] =
    Future {
      DB.localTx { implicit session =>
        val u = userMySqlRepository.syntax("u")

        withSQL {
          select(u.result.*)
            .from(userMySqlRepository as u)
            .innerJoin(this as ru)
            .on(ru.userId, u.id)
            .where
            .eq(ru.resourceId, resourceId.id)
            .and
            .eq(ru.resourceUserType, ResourceUserType.OWNER.toString)
            .and
            .eq(ru.deleted, false)
        }.map(userMySqlRepository.apply(u.resultName)(_)).single()
      }
    }

  override def deleteAllResourceByUserId(userId: UserId): Future[Done] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this as ru)
            .set(column.deleted -> true)
            .where
            .eq(ru.userId, userId.id)
        }.update()
      }
      Done
    }
  }
}
