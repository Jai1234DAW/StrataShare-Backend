package dev.pompilius.studies.domain

import com.google.inject.ImplementedBy
import dev.pompilius.studies.infrastructure.repositories.StudyAttachmentMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[StudyAttachmentMySqlRepository])
trait StudyAttachmentRepository {

  def addAttachment(studyId: StudyId, attachmentId: dev.pompilius.attachment.domain.AttachmentId): Future[Done]

  def removeAttachment(studyId: StudyId, attachmentId: dev.pompilius.attachment.domain.AttachmentId): Future[Done]

  def getAttachments(studyId: StudyId): Future[List[dev.pompilius.attachment.domain.AttachmentId]]

  def deleteAllByStudy(studyId: StudyId): Future[Done]

}

