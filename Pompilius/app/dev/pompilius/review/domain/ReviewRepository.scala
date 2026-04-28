package dev.pompilius.review.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.review.infrastructure.repositories.ReviewMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scala.concurrent.Future


@ImplementedBy(classOf[ReviewMySqlRepository])
trait ReviewRepository {
  def save(review: Review): Future[Done]
  def findById(id: ReviewId): Future[Option[Review]]
  def findByResource(resourceId: ResourceId, pag: Pagination): Future[List[Review]]
  //def findByUser(userId: UserId, pag: Pagination): Future[List[Review]]
  def findByResourceAndUser(resourceId: ResourceId, userId: UserId): Future[Option[Review]]
  def getAverageRating(resourceId: ResourceId): Future[Double]
  def getCommentsCount(resourceId: ResourceId): Future[Int]
  def getReviewCount(resourceId: ResourceId): Future[Int]
  def delete(id: ReviewId): Future[Done]
}
