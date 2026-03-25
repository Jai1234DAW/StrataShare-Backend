package dev.pompilius.study.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.studies.infrastructure.repositories.StudyMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudyMySqlRepository])
trait StudyRepository {

  def getById(studyId: StudyId): Future[Study]

  def findById(studyId: StudyId): Future[Option[Study]]

  def findByName(name: String): Future[List[Study]]

  def find(filter: StudyFilter, pag: Pagination): Future[List[Study]]

  def save(study: Study): Future[Done]

  def delete(studyId: StudyId): Future[Done]

}

