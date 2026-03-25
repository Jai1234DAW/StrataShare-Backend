package dev.pompilius.studies.infrastructure.repositories

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.studies.domain.{StudyAttachment, StudyAttachmentRepository, StudyId}
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StudyAttachmentMySqlRepository @Inject() (

)(implicit dbExecutionContext: DbExecutionContext)
    extends StudyAttachmentRepository
    with SQLSyntaxSupport[StudyAttachment] {

  override val tableName = "studies_attachments"

  def apply(sa: SyntaxProvider[StudyAttachment])(rs: WrappedResultSet): StudyAttachment =
    apply(sa.resultName)(rs)

  def apply(sa: ResultName[StudyAttachment])(rs: WrappedResultSet): StudyAttachment =
    StudyAttachment(
      studyId = StudyId(rs.get[Long](sa.studyId)),
      attachmentId = AttachmentId(rs.get[Long](sa.attachmentId))
    )

  private val sa = this.syntax("sa")

  override def addAttachment(studyId: StudyId, attachmentId: AttachmentId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          insert.into(this).namedValues(
            column.studyId -> studyId.id,
            column.attachmentId -> attachmentId.id
          )
        }.execute()
      }
      Done
    }

  override def removeAttachment(studyId: StudyId, attachmentId: AttachmentId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where
            .eq(sa.studyId, studyId.id)
            .and.eq(sa.attachmentId, attachmentId.id)
        }.execute()
      }
      Done
    }

  override def getAttachments(studyId: StudyId): Future[List[AttachmentId]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as sa).where.eq(sa.studyId, studyId.id)
        }.map(rs => AttachmentId(rs.get[Long](sa.attachmentId))).list()
      }
    }

  override def deleteAllByStudy(studyId: StudyId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(sa.studyId, studyId.id)
        }.execute()
      }
      Done
    }
}

