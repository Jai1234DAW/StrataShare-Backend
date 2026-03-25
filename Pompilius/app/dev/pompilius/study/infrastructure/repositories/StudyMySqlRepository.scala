package dev.pompilius.studies.infrastructure.repositories

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.studies.domain.exceptions.StudyNotFoundException
import dev.pompilius.studies.domain.{Study, StudyFilter, StudyId, StudyRepository}
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StudyMySqlRepository @Inject() (

)(implicit dbExecutionContext: DbExecutionContext)
    extends StudyRepository
    with SQLSyntaxSupport[Study] {

  override val tableName = "studies"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(s: SyntaxProvider[Study])(rs: WrappedResultSet): Study =
    apply(s.resultName)(rs)

  def apply(s: ResultName[Study])(rs: WrappedResultSet): Study =
    Study(
      id = StudyId(rs.get[Long](s.id)),
      name = rs.get(s.name),
      visibility = rs.get(s.visibility),
      localization = rs.get(s.localization),
      startDate = rs.get(s.startDate),
      endDate = rs.get(s.endDate),
      description = rs.get(s.description),
      coordinates = rs.get(s.coordinates),
      observations = rs.get(s.observations),
      summary = rs.get(s.summary),
      created = rs.get(s.created),
      updated = rs.get(s.updated),
      attachments = List(), // Se cargarán desde otra tabla si es necesario
      samples = List() // Se cargarán desde otra tabla si es necesario
    )

  private val s = this.syntax("s")

  override def getById(studyId: StudyId): Future[Study] = {
    findById(studyId).map(_.getOrElse(throw new StudyNotFoundException(s"Study with id ${studyId.toString} not found")))
  }

  override def findById(studyId: StudyId): Future[Option[Study]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.eq(s.id, studyId.id)
        }.map(apply(s.resultName)(_)).single()
      }
    }

  override def findByName(name: String): Future[List[Study]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.like(s.name, s"%$name%")
        }.map(apply(s.resultName)(_)).list()
      }
    }

  override def find(filter: StudyFilter, pag: Pagination): Future[List[Study]] =
    Future {
      DB.localTx { implicit session =>
        val query = selectFrom(this as s).where

        val conditions = ScalikeUtil.buildConditions(
          filter.name.map(name => sqls"${s.name} LIKE ${s"%$name%"}"),
          filter.visibility.map(visibility => sqls"${s.visibility} = $visibility"),
          filter.localization.map(localization => sqls"${s.localization} LIKE ${s"%$localization%"}"),
          filter.startDateFrom.map(date => sqls"${s.startDate} >= $date"),
          filter.startDateTo.map(date => sqls"${s.startDate} <= $date"),
          filter.endDateFrom.map(date => sqls"${s.endDate} >= $date"),
          filter.endDateTo.map(date => sqls"${s.endDate} <= $date")
        )

        withSQL {
          select.from(this as s).where(conditions).orderBy(s.created.desc).limit(pag.limit).offset(pag.offset)
        }.map(apply(s.resultName)(_)).list()
      }
    }

  override def save(study: Study): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          if (study.id.id == 0) {
            insert.into(this).namedValues(
              column.id -> study.id.id,
              column.name -> study.name,
              column.visibility -> study.visibility,
              column.localization -> study.localization,
              column.startDate -> study.startDate,
              column.endDate -> study.endDate,
              column.description -> study.description,
              column.coordinates -> study.coordinates,
              column.observations -> study.observations,
              column.summary -> study.summary,
              column.created -> study.created,
              column.updated -> study.updated
            )
          } else {
            update(this).set(
              column.name -> study.name,
              column.visibility -> study.visibility,
              column.localization -> study.localization,
              column.startDate -> study.startDate,
              column.endDate -> study.endDate,
              column.description -> study.description,
              column.coordinates -> study.coordinates,
              column.observations -> study.observations,
              column.summary -> study.summary,
              column.updated -> study.updated
            ).where.eq(column.id, study.id.id)
          }
        }.execute()
      }
      Done
    }

  override def delete(studyId: StudyId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.id, studyId.id)
        }.execute()
      }
      Done
    }
}

