package dev.pompilius.study.domain

import com.google.inject.ImplementedBy
import dev.pompilius.study.infrastructure.repositories.StudySampleMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudySampleMySqlRepository])
trait StudySampleRepository {

  def addSample(studyId: StudyId, sampleId: String): Future[Done]

  def removeSample(studyId: StudyId, sampleId: String): Future[Done]

  def getSamples(studyId: StudyId): Future[List[String]]

  def deleteAllByStudy(studyId: StudyId): Future[Done]

}

