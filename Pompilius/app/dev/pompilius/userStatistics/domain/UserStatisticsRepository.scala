package dev.pompilius.userStatistics.domain

import com.google.inject.ImplementedBy
import dev.pompilius.userStatistics.infrastructure.repositories.UserStatisticsMySqlRepository
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserStatisticsMySqlRepository])
trait UserStatisticsRepository {

  def save(stats: UserStatistics): Future[Done]

  def findByUserId(userId: UserId): Future[Option[UserStatistics]]
}
