package dev.pompilius.resource.infrastructure.repositories.study

import dev.pompilius.Strings
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.resource.domain.sample.SampleId
import dev.pompilius.resource.domain.study._
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class StudySampleMySqlRepository @Inject()(
)(implicit dbExecutionContext: DbExecutionContext)
    extends StudySampleRepository
    with SQLSyntaxSupport[StudySample] {

  override val tableName = "study_sample"

  def apply(ss: SyntaxProvider[StudySample])(rs: WrappedResultSet): StudySample =
    apply(ss.resultName)(rs)

  def apply(ss: ResultName[StudySample])(rs: WrappedResultSet): StudySample =
    StudySample(
      studyId = StudyId(rs.get[Long](ss.studyId)),
      sampleId = SampleId(rs.get[Long](ss.sampleId))
    )

  private val ss = this.syntax("ss")

  override def getAllByStudyId(studyId: StudyId): Future[List[StudySample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ss).where.eq(ss.studyId, studyId.id)
        }.map(apply(ss.resultName)(_)).list()
      }
    }


  override def saveMultiple(studySamples: List[StudySample]): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        studySamples.foreach { ss =>
          val values = List(
            column.studyId -> ss.studyId.id,
            column.sampleId -> ss.sampleId.id
          )
          withSQL {
            insert
              .into(this)
              .namedValues(values: _*)
          }.update()
        }
      }
      Done
    }


  override def delete(studyId: StudyId, sampleId: SampleId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(ss.studyId, studyId.id).and.eq(ss.sampleId, sampleId.id)
        }.update()
      }
      Done
    }

  override def find(filter: StudySampleFilter, pag: Pagination): Future[List[StudySample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as ss)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(ss.sutdyId, ss.sampleId)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(ss.resultName)(_)).list()
      }
    }

  private def filterToSqlSyntax(filter: StudySampleFilter): Option[SQLSyntax] = {

    val filters = List(
      filter.studyId.map(id => sqls.eq(ss.studyId, id.id)),
      filter.sampleId.map(id => sqls.eq(ss.sampleId, id.id))
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }
}



