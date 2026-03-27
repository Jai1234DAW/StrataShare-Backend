package dev.pompilius.resource.domain.study

import com.google.inject.ImplementedBy
import dev.pompilius.resource.infrastructure.repositories.study.StudySampleMySqlRepository
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done
import dev.pompilius.resource.domain.sample.SampleId

import scala.concurrent.Future

@ImplementedBy(classOf[StudySampleMySqlRepository])
trait StudySampleRepository {

  def getAllByStudyId(studyId: StudyId): Future[List[StudySample]]

  def find(filter: StudySampleFilter, pag: Pagination): Future[List[StudySample]]

  def saveMultiple(studySamples: List[StudySample]): Future[Done]

  def delete(studyId: StudyId, sampleId: SampleId): Future[Done]
}