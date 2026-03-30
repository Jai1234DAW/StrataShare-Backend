package dev.pompilius.study.domain

import com.google.inject.ImplementedBy
import dev.pompilius.sample.domain.SampleId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.study.infrastructure.repositories.StudySampleMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudySampleMySqlRepository])
trait StudySampleRepository {

  def getAllByStudyId(studyId: StudyId): Future[List[StudySample]]

  def find(filter: StudySampleFilter, pag: Pagination): Future[List[StudySample]]

  def saveMultiple(studySamples: List[StudySample]): Future[Done]

  def delete(studyId: StudyId, sampleId: SampleId): Future[Done]
}