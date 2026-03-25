package dev.pompilius.sample.infrastructure.repositories

import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.sample.domain.exceptions.SampleNotFoundException
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SampleMySqlRepository @Inject() (

)(implicit dbExecutionContext: DbExecutionContext)
    extends SampleRepository
    with SQLSyntaxSupport[Sample] {

  override val tableName = "samples"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(s: SyntaxProvider[Sample])(rs: WrappedResultSet): Sample =
    apply(s.resultName)(rs)

  def apply(s: ResultName[Sample])(rs: WrappedResultSet): Sample =
    Sample(
      id = SampleId(rs.get[Long](s.id)),
      name = rs.get(s.name),
      description = rs.get(s.description),
      minerals = rs.get(s.minerals),
      collectionMethods = rs.get(s.collectionMethods),
      isFresh = rs.get(s.isFresh),
      sampleType = rs.get(s.sampleType),
      materialsUsed = rs.get(s.materialsUsed),
      rockType = rs.get(s.rockType),
      geologicalProcesses = rs.get(s.geologicalProcesses),
      created = rs.get(s.created),
      updated = rs.get(s.updated)
    )

  private val s = this.syntax("s")

  override def getById(sampleId: SampleId): Future[Sample] = {
    findById(sampleId).map(_.getOrElse(throw new SampleNotFoundException(s"Sample with id ${sampleId.toString} not found")))
  }

  override def findById(sampleId: SampleId): Future[Option[Sample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.eq(s.id, sampleId.id)
        }.map(apply(s.resultName)(_)).single()
      }
    }

  override def findByName(name: String): Future[List[Sample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.like(s.name, s"%$name%")
        }.map(apply(s.resultName)(_)).list()
      }
    }

  override def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]] =
    Future {
      DB.localTx { implicit session =>
        val conditions = ScalikeUtil.buildConditions(
          filter.name.map(name => sqls"${s.name} LIKE ${s"%$name%"}"),
          filter.sampleType.map(sampleType => sqls"${s.sampleType} = $sampleType"),
          filter.rockType.map(rockType => sqls"${s.rockType} LIKE ${s"%$rockType%"}"),
          filter.isFresh.map(isFresh => sqls"${s.isFresh} = $isFresh")
        )

        withSQL {
          select.from(this as s).where(conditions).orderBy(s.created.desc).limit(pag.limit).offset(pag.offset)
        }.map(apply(s.resultName)(_)).list()
      }
    }

  override def save(sample: Sample): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          if (sample.id.id == 0) {
            insert.into(this).namedValues(
              column.id -> sample.id.id,
              column.name -> sample.name,
              column.description -> sample.description,
              column.minerals -> sample.minerals,
              column.collectionMethods -> sample.collectionMethods,
              column.isFresh -> sample.isFresh,
              column.sampleType -> sample.sampleType,
              column.materialsUsed -> sample.materialsUsed,
              column.rockType -> sample.rockType,
              column.geologicalProcesses -> sample.geologicalProcesses,
              column.created -> sample.created,
              column.updated -> sample.updated
            )
          } else {
            update(this).set(
              column.name -> sample.name,
              column.description -> sample.description,
              column.minerals -> sample.minerals,
              column.collectionMethods -> sample.collectionMethods,
              column.isFresh -> sample.isFresh,
              column.sampleType -> sample.sampleType,
              column.materialsUsed -> sample.materialsUsed,
              column.rockType -> sample.rockType,
              column.geologicalProcesses -> sample.geologicalProcesses,
              column.updated -> sample.updated
            ).where.eq(column.id, sample.id.id)
          }
        }.execute()
      }
      Done
    }

  override def delete(sampleId: SampleId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.id, sampleId.id)
        }.execute()
      }
      Done
    }
}

