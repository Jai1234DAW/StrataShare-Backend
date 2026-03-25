package dev.pompilius.study.infrastructure.repositories

import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.study.domain.{StudySample, StudySampleRepository, StudyId}
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StudySampleMySqlRepository @Inject() (

)(implicit dbExecutionContext: DbExecutionContext)
    extends StudySampleRepository
    with SQLSyntaxSupport[StudySample] {

  override val tableName = "study_samples"

  def apply(ss: SyntaxProvider[StudySample])(rs: WrappedResultSet): StudySample =
    apply(ss.resultName)(rs)

  def apply(ss: ResultName[StudySample])(rs: WrappedResultSet): StudySample =
    StudySample(
      studyId = StudyId(rs.get[Long](ss.studyId)),
      sampleId = rs.get(ss.sampleId)
    )

  private val ss = this.syntax("ss")

  override def addSample(studyId: StudyId, sampleId: String): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          insert.into(this).namedValues(
            column.studyId -> studyId.id,
            column.sampleId -> sampleId
          )
        }.execute()
      }
      Done
    }

  override def removeSample(studyId: StudyId, sampleId: String): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where
            .eq(ss.studyId, studyId.id)
            .and.eq(ss.sampleId, sampleId)
        }.execute()
      }
      Done
    }

  override def getSamples(studyId: StudyId): Future[List[String]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ss).where.eq(ss.studyId, studyId.id)
        }.map(rs => rs.get[String](ss.sampleId)).list()
      }
    }

  override def deleteAllByStudy(studyId: StudyId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(ss.studyId, studyId.id)
        }.execute()
      }
      Done
    }
}

