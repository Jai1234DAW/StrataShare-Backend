package dev.pompilius.badge.domain

import com.google.inject.ImplementedBy
import dev.pompilius.badge.infrastructure.repositories.BadgeMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[BadgeMySqlRepository])
trait BadgeRepository {

  def save(badge: Badge): Future[Done]

  def findById(badgeId: BadgeId): Future[Option[Badge]]

  def findByType(badgeType: BadgeType): Future[Option[Badge]]

  def findAll: Future[List[Badge]]

  def delete(badgeId: BadgeId): Future[Done]
}
