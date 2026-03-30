package dev.pompilius.sample.infrastructure.repositories

import dev.pompilius.Strings
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class SampleMySqlRepository @Inject() (
)(implicit dbExecutionContext: DbExecutionContext)
    extends SampleRepository
    with SQLSyntaxSupport[Sample] {

  override val tableName = "sample"

  def apply(s: SyntaxProvider[Sample])(rs: WrappedResultSet): Sample =
    apply(s.resultName)(rs)

  def apply(s: ResultName[Sample])(rs: WrappedResultSet): Sample =
    Sample(
      id = SampleId(rs.get[Long](s.id)),
      resourceId=ResourceId(rs.get[Long](s.resourceId)),
      name = rs.get(s.name),
      minerals = rs.get(s.minerals),
      collectionMethods = rs.get(s.collectionMethods),
      isFresh = rs.get(s.isFresh),
      sampleType = rs.get(s.sampleType),
      materialsUsed = rs.get(s.materialsUsed),
      rockType = rs.get(s.rockType),
      geologicalProcesses = rs.get(s.geologicalProcesses)
    )

  private val s = this.syntax("s")

  override def findById(id: SampleId): Future[Option[Sample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.eq(s.id, id.id)
        }.map(apply(s.resultName)(_)).single()
      }
    }

  override def findByResource(resourceId: ResourceId): Future[Option[Sample]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s).where.eq(s.resourceId, resourceId.id)
        }.map(apply(s.resultName)(_)).single()
      }
    }

  override def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]] =
    Future {
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(orderBy: _*)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(s.resultName)(_)).list()
      }
    }

  // Mirar si hago borrado lógico o físico. Por ahora, borrado físico
  override def delete(id: SampleId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(s.id, id.id)
        }.update()
      }
      Done
    }

  private def filterToSqlSyntax(filter: SampleFilter): Option[SQLSyntax] = {
    val nameFilter = filter.name.map { name =>
      sqls.like(sqls.lower(s.name), s"%${name.toLowerCase}%")
    }

    val sampleTypeFilter = filter.sampleType.map { sampleType =>
      sqls.eq(sqls.lower(s.sampleType), sampleType.toLowerCase)
    }

    val rockTypeFilter = filter.rockType.map { rockType =>
      sqls.eq(sqls.lower(s.rockType), rockType.toLowerCase)
    }

    val isFreshFilter = filter.isFresh.map { isFresh =>
      sqls.eq(s.isFresh, isFresh)
    }

    val filters = List(
      nameFilter,
      sampleTypeFilter,
      rockTypeFilter,
      isFreshFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  private def buildOrderBy(pag: Pagination): Seq[SQLSyntax] = {
    val defaultOrderBy = Seq(s.name, s.id.desc)

    pag.orderBy match {
      case Nil =>
        defaultOrderBy
      case seq =>
        seq.flatMap { field =>
          val desc = field.startsWith("-")
          field.stripPrefix("-") match {
            case Strings.name =>
              Some(if (desc) s.name.desc else s.name.asc)
            case _ =>
              None
          }
        } match {
          case Nil =>
            defaultOrderBy
          case e =>
            e.appended(s.id.desc)
        }
    }
  }

  override def save(sample: Sample): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val sampleValues = List(
          column.id -> sample.id.id,
          column.resourceId -> sample.resourceId.id,
          column.name -> sample.name,
          column.minerals -> sample.minerals,
          column.collectionMethods -> sample.collectionMethods,
          column.isFresh -> sample.isFresh,
          column.sampleType -> sample.sampleType,
          column.materialsUsed -> sample.materialsUsed,
          column.rockType -> sample.rockType,
          column.geologicalProcesses -> sample.geologicalProcesses
        )
        withSQL {
          insert
            .into(this)
            .namedValues(sampleValues: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, sampleValues: _*))
        }.update()
      }
      Done
    }

}
