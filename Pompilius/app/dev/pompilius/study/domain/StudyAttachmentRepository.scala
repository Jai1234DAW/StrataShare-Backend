package dev.pompilius.study.domain

import com.google.inject.ImplementedBy
import dev.pompilius.study.infrastructure.repositories.StudyAttachmentMySqlRepository
import org.apache.pekko.Done
import dev.pompilius.attachment.AttachmentId

import scala.concurrent.Future

@ImplementedBy(classOf[StudyAttachmentMySqlRepository])
trait StudyAttachmentRepository {

  def addAttachment(studyId: StudyId, attachmentId:AttachmentId): Future[Done]

  def removeAttachment(studyId: StudyId, attachmentId:AttachmentId): Future[Done]

  def getAttachments(studyId: StudyId): Future[List[AttachmentId]]

  def deleteAllByStudy(studyId: StudyId): Future[Done]

}

