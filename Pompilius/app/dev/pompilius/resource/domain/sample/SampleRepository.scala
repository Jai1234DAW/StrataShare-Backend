package dev.pompilius.resource.domain.sample

import com.google.inject.ImplementedBy
import dev.pompilius.resource.infrastructure.repositories.study.StudyMySqlRepository
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudyMySqlRepository])
trait SampleRepository {

  def findById(id: SampleId): Future[Option[Sample]]

  def find(filter: SampleFilter, pag: Pagination): Future[Seq[Sample]]

  def save(sample: Sample): Future[Done]
}
