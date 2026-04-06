package dev.pompilius.attachment.domain

import com.google.inject.ImplementedBy
import dev.pompilius.attachment.infrastructure.repositories.AttachmentMySqlRepository
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[AttachmentMySqlRepository])
trait AttachmentRepository {
  def findById(id: AttachmentId): Future[Option[Attachment]]
  def save(attachment: Attachment): Future[Done]
  def delete(id: AttachmentId): Future[Done]
  def findByResourceId(resourceId: ResourceId, pag:Pagination): Future[List[Attachment]]
}
