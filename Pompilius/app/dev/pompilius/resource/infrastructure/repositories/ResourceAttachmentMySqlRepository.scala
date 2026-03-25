package dev.pompilius.resource.infrastructure.repositories

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.resource.domain.{ResourceAttachment, ResourceAttachmentRepository, ResourceId}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ResourceAttachmentMySqlRepository @Inject() (
)(implicit dbExecutionContext: DbExecutionContext)
    extends ResourceAttachmentRepository
    with SQLSyntaxSupport[ResourceAttachment] {

  override val tableName = "resource_attachment"

  private val ra = this.syntax("ra")

  override def add(resourceAttachment: ResourceAttachment): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.resourceId -> resourceAttachment.resourceId.id,
            column.attachmentId -> resourceAttachment.attachmentId.id
        )
        withSQL {
          insert.into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(values: _*))
        }.update()
      }
      Done
    }


  override def remove(resourceAttachment: ResourceAttachment): Future[Done] = {
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.resourceId, resourceAttachment.resourceId.id).and.eq(column.attachmentId, resourceAttachment.attachmentId.id)
        }.update()
      }
      Done
    }
  }


override def getAttachments(resourceId: ResourceId): Future[Seq[AttachmentId]] =
  Future {
    DB.readOnly { implicit session =>
      withSQL {
        selectFrom(this as ra).where.eq(ra.resourceId, resourceId.id)
      }.map(rs => AttachmentId(rs.get[Long](ra.attachmentId))).list()
    }
  }

  override def deleteAllByResource(resourceId: ResourceId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(ra.resourceId, resourceId.id)
        }.execute()
      }
      Done
    }
}

