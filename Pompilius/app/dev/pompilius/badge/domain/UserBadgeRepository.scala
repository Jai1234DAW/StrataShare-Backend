package dev.pompilius.badge.domain

import com.google.inject.ImplementedBy
import dev.pompilius.badge.infrastructure.repositories.UserBadgeMySqlRepository
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserBadgeMySqlRepository])
trait UserBadgeRepository {

  def save(userBadge: UserBadge): Future[Done]

  def findByUserIdAndBadgeId(userId: UserId, badgeId: BadgeId): Future[Option[UserBadge]]

  def findAllByUserId(userId: UserId): Future[List[UserBadge]]

  def hasUserEarnedBadge(userId: UserId, badgeType: BadgeType): Future[Boolean]
}

