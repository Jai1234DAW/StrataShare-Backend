package dev.pompilius.resource.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done
import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.resource.infrastructure.repositories.ResourceAttachmentMySqlRepository

import scala.concurrent.Future

@ImplementedBy(classOf[ResourceAttachmentMySqlRepository])
trait ResourceAttachmentRepository {

  def add(resourceAttachment: ResourceAttachment): Future[Done]

  def remove(resourceAttachment: ResourceAttachment): Future[Done]

  def getAttachments(resourceId: ResourceId): Future[Seq[AttachmentId]]

  def deleteAllByResource(resourceId: ResourceId): Future[Done]
}

