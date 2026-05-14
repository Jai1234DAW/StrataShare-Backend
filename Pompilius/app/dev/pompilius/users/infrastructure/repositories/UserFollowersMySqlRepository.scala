package dev.pompilius.users.infrastructure.repositories

import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain._
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scalikejdbc.jodatime.JodaParameterBinderFactory._

@Singleton
class UserFollowersMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends UserFollowersRepository
    with SQLSyntaxSupport[UserFollower] {


  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))
  override val tableName = "users_followers"

  def apply(uf: SyntaxProvider[UserFollower])(rs: WrappedResultSet): UserFollower =
    apply(uf.resultName)(rs)

  def apply(uf: ResultName[UserFollower])(rs: WrappedResultSet): UserFollower =
    UserFollower(
      userId = UserId(rs.get[Long](uf.userId)),
      followerId = UserId(rs.get[Long](uf.followerId)),
      followedAt = rs.get(uf.followedAt)
    )

  private val uf = this.syntax("uf")

  override def getAllByUserId(userId: UserId, pag: Pagination): Future[List[UserFollower]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as uf).where.eq(uf.userId, userId.id).orderBy(uf.followedAt).desc.append(ScalikeUtil.pag(pag))
        }.map(apply(uf.resultName)(_)).list()
      }
    }

  override def save(userFollower: UserFollower): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.userId -> userFollower.userId.id,
          column.followerId -> userFollower.followerId.id,
          column.followedAt -> userFollower.followedAt
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

  override def delete(followerId: UserId, userId: UserId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this as uf).where
            .eq(uf.userId, userId.id)
            .and
            .eq(uf.followerId, followerId.id)
        }.update()
      }
      Done
    }

  override def countByUserId(userId: UserId): Future[Int] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          select(sqls.count).from(this as uf).where.eq(uf.userId, userId.id)
        }.map(_.int(1)).single().getOrElse(0)
      }
    }

//  override def isFollower(userId: UserId, followerId: UserId): Future[Boolean] =
//    Future {
//      DB.localTx { implicit session =>
//        withSQL {
//          select(sqls.count)
//            .from(this as uf)
//            .where
//            .eq(uf.userId, userId.id)
//            .and
//            .eq(uf.followerId, followerId.id)
//        }.map(_.int(1)).single().getOrElse(0) > 0
//      }
//    }

  override def isFollower(userId: UserId, followerId: UserId): Future[Boolean] =
    Future {
      DB.readOnly { implicit session =>
        withSQL {
          select(sqls"1")
            .from(this as uf)
            .where
            .eq(uf.userId, userId.id)
            .and
            .eq(uf.followerId, followerId.id)
            .limit(1)
        }.map(_ => true).single().getOrElse(false)
      }
    }
}
