package dev.pompilius.users.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.infrastructure.repositories.UserFollowersMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[UserFollowersMySqlRepository])
trait UserFollowersRepository {

  def getAllByUserId(userId:UserId, pag:Pagination): Future[List[UserFollower]]

  def save(userFollower: UserFollower): Future[Done]

  def delete(followerId: UserId, userId: UserId): Future[Done]

  def countByUserId(userId: UserId): Future[Int]

  def isFollower(userId: UserId, followerId: UserId): Future[Boolean]
}