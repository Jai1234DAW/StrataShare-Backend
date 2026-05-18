package dev.pompilius.users.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done
import dev.pompilius.attachment.domain._
import dev.pompilius.users.infrastructure.repositories.UserAttachmentMySqlRepository

import scala.concurrent.Future

@ImplementedBy(classOf[UserAttachmentMySqlRepository])
trait UserAttachmentRepository {
  def findBy(userId: UserId, attachmentId: AttachmentId): Future[Option[UserAttachment]]
  def find(filter: UserAttachmentFilter, pag: Pagination): Future[List[UserAttachment]]
  def save(userAttachment: UserAttachment): Future[Done]
  def delete(userId: UserId, attachmentId: AttachmentId): Future[Done]

  def findCurrentByType(userId: UserId, attachmentType: UserAttachmentType): Future[Option[UserAttachment]]
  def markAllAsNotCurrent(userId: UserId, attachmentType: UserAttachmentType): Future[Done]
}
