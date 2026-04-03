package dev.pompilius.attachment.infrastructure.repositories

import dev.pompilius.attachment.domain.{Attachment, AttachmentId, AttachmentRepository}
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import dev.pompilius.shared.infrastructure.ScalikeUtil

@Singleton
class AttachmentMySqlRepository @Inject() (implicit ec: DbExecutionContext)
    extends AttachmentRepository
    with SQLSyntaxSupport[Attachment] {

  override val tableName = "attachment"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(att: SyntaxProvider[Attachment])(rs: WrappedResultSet): Attachment =
    apply(att.resultName)(rs)

  def apply(att: ResultName[Attachment])(rs: WrappedResultSet): Attachment =
    Attachment(
      id = AttachmentId(rs.get[Long](att.id)),
      node = rs.get(att.node),
      relativePath = rs.get(att.relativePath),
      filename = rs.get(att.filename),
      description = rs.get(att.description),
      contentType = rs.get(att.contentType),
      size = rs.get(att.size),
      createdAt = rs.get(att.createdAt),
      isPublic = rs.get(att.isPublic),
      deleted = rs.get(att.deleted),
      metadata = rs.get(att.metadata),
      resourceId = rs.getOpt[Long](att.resourceId).map(ResourceId(_))
    )

  private val att = this.syntax("att")

  override def findById(id: AttachmentId): Future[Option[Attachment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as att).where.eq(att.id, id.id)
        }.map(apply(att.resultName)(_)).single()
      }
    }

  override def save(attachment: Attachment): Future[Done] =
    Future {
      DB.localTx { implicit dbSession =>
        val values = List(
          column.id -> attachment.id.id,
          column.node -> attachment.node,
          column.relativePath -> attachment.relativePath,
          column.filename -> attachment.filename,
          column.description -> attachment.description,
          column.contentType -> attachment.contentType,
          column.size -> attachment.size,
          column.createdAt -> attachment.createdAt,
          column.isPublic -> attachment.isPublic,
          column.deleted -> attachment.deleted,
          column.metadata -> attachment.metadata,
          column.resourceId -> attachment.resourceId.map(_.id)
        )

        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, values: _*))
        }.update()
      }
      Done
    }

  override def delete(id: AttachmentId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this as att).set(column.deleted -> true).where.eq(att.id, id.id)
        }.update()
      }
      Done
    }

  override def findByResourceId(resourceId: ResourceId): Future[List[Attachment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as att).where.eq(att.resourceId, resourceId.id).and.eq(att.deleted, false)
        }.map(apply(att.resultName)(_)).list()
      }
    }

}
