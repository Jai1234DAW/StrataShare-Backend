package dev.pompilius.resource.domain.sample

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.sample.{SampleFilter,Sample, SampleId}
import dev.pompilius.resource.infrastructure.repositories.sample.SampleMySqlRepository
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[SampleMySqlRepository])
trait SampleRepository {

  def findById(id: SampleId): Future[Option[Sample]]

  def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]]

  def save(sample: Sample): Future[Done]
}
