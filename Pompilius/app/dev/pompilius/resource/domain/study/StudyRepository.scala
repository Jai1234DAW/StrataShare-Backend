package dev.pompilius.resource.domain.study

import com.google.inject.ImplementedBy
import dev.pompilius.resource.domain.study.{StudyFilter, StudyId, Study}
import dev.pompilius.resource.infrastructure.repositories.study.StudyMySqlRepository
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudyMySqlRepository])
trait StudyRepository {

  def findById(id: StudyId): Future[Option[Study]]

  def find(filter: StudyFilter, pag: Pagination): Future[Seq[Study]]

  def save(study: Study): Future[Done]
}
