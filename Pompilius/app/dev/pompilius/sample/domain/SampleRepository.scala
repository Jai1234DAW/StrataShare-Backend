package dev.pompilius.sample.domain

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.sample.infrastructure.repositories.SampleMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[SampleMySqlRepository])
trait SampleRepository {

  def findById(id: SampleId): Future[Option[Sample]]

  def findByResource(resourceId: ResourceId): Future[Option[Sample]]

  def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]]

  def save(sample: Sample): Future[Done]

  def delete(id: SampleId): Future[Done]

  def getMyAllSamplesAs(userId:UserId, pag: Pagination, userType: String): Future[List[Sample]]
}
