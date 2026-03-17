package dev.pompilius.attachments.domain

import com.google.inject.ImplementedBy
import dev.pompilius.attachments.infrastructure.repositories.AttachmentMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[AttachmentMySqlRepository])
trait AttachmentRepository {
  def findById(id: AttachmentId): Future[Option[Attachment]]
  def save(attachment: Attachment): Future[Done]
  def delete(id: AttachmentId): Future[Done]
}
