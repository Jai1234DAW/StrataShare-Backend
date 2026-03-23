package dev.pompilius.attachment.infrastructure.cache

import dev.pompilius.attachment.domain._
import javax.inject.{Inject, Singleton}
import dev.pompilius.attachment.infrastructure.repositories.AttachmentMySqlRepository
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import play.api.cache.AsyncCacheApi
import scala.concurrent.Future
import scala.concurrent.duration._

@Singleton
class AttachmentCachedRepository @Inject() (cache: AsyncCacheApi)(implicit dbExecutionContext: DbExecutionContext)
    extends AttachmentMySqlRepository
    with AttachmentRepository {

  override def findById(attachmentId: AttachmentId): Future[Option[Attachment]] = {
    val key = s"attachment:${attachmentId.toString}"
    cache.get[Attachment](key).flatMap {
      case Some(attachment) =>
        Future.successful(Some(attachment))
      case _ =>
        super.findById(attachmentId).flatMap {
          case Some(attachment) =>
            cache.set(key, attachment, 6.hours).map(_ => Some(attachment))
          case _ =>
            Future.successful(None)
        }
    }
  }
}
