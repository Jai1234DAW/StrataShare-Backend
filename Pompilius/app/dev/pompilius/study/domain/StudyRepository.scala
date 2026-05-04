package dev.pompilius.study.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.study.infrastructure.repositories.StudyMySqlRepository
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudyMySqlRepository])
trait StudyRepository {

  def findById(id: StudyId): Future[Option[Study]]

  def find(filter: StudyFilter, pag: Pagination): Future[List[Study]]

  def findByResource(resourceId: ResourceId): Future[Option[Study]]

  def save(study: Study): Future[Done]

  def delete(id: StudyId): Future[Done]

  def getMyAllStudiesAs(userId:UserId, pag: Pagination, user_type: String): Future[List[Study]]
}
