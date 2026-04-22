package dev.pompilius.badge.infrastructure.repositories

import dev.pompilius.badge.domain._
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
class UserBadgeMySqlRepository @Inject() (badgeMySqlRepository: BadgeMySqlRepository)(implicit dbExecutionContext: DbExecutionContext)
    extends UserBadgeRepository
    with SQLSyntaxSupport[UserBadge] {

  override val tableName = "users_badge"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(ub: SyntaxProvider[UserBadge])(rs: WrappedResultSet): UserBadge =
    apply(ub.resultName)(rs)

  def apply(ub: ResultName[UserBadge])(rs: WrappedResultSet): UserBadge =
    UserBadge(
      userId = UserId(rs.get[Long](ub.userId)),
      badgeId = BadgeId(rs.get[Long](ub.badgeId)),
      earnedAt = rs.get(ub.earnedAt)
    )

  private val ub = this.syntax("ub")

  override def save(userBadge: UserBadge): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          insert
            .into(this)
            .namedValues(
              column.userId -> userBadge.userId.id,
              column.badgeId -> userBadge.badgeId.id,
              column.earnedAt -> userBadge.earnedAt
            )
        }.update()
      }
      Done
    }

  override def findByUserIdAndBadgeId(userId: UserId, badgeId: BadgeId): Future[Option[UserBadge]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ub)
            .where
            .eq(ub.userId, userId.id)
            .and
            .eq(ub.badgeId, badgeId.id)
        }.map(apply(ub.resultName)(_)).single()
      }
    }

  override def findAllByUserId(userId: UserId): Future[List[UserBadge]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ub)
            .where
            .eq(ub.userId, userId.id)
            .orderBy(ub.earnedAt)
            .desc
        }.map(apply(ub.resultName)(_)).list()
      }
    }

  override def hasUserEarnedBadge(userId: UserId, badgeType: BadgeType): Future[Boolean] =
    Future {
      DB.localTx { implicit session =>
        val b = badgeMySqlRepository.syntax("b")
        withSQL {
          select(sqls.count)
            .from(this as ub)
            .innerJoin(badgeMySqlRepository as b)
            .on(sqls.eq(ub.badgeId, b.id))
            .where
            .eq(ub.userId, userId.id)
            .and
            .eq(b.badgeType, badgeType.value)
        }.map(rs => rs.int(1)).single().exists(_ > 0)
      }
    }
}





