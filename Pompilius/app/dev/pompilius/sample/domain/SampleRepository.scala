package dev.pompilius.sample.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.sample.infrastructure.repositories.SampleMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[SampleMySqlRepository])
trait SampleRepository {

  def getById(sampleId: SampleId): Future[Sample]

  def findById(sampleId: SampleId): Future[Option[Sample]]

  def findByName(name: String): Future[List[Sample]]

  def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]]

  def save(sample: Sample): Future[Done]

  def delete(sampleId: SampleId): Future[Done]

}

