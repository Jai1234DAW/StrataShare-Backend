package dev.pompilius.sample.infrastructure.repositories

import dev.pompilius.Strings
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.resource.infrastructure.repositories.{ResourceMySqlRepository, ResourceUserMySqlRepository}
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
    resourceUserMySqlRepository: ResourceUserMySqlRepository,
    resourceMySqlRepository: ResourceMySqlRepository
)(implicit dbExecutionContext: DbExecutionContext)
    extends SampleRepository
    with SQLSyntaxSupport[Sample] {

  override val tableName = "sample"

  def apply(s: SyntaxProvider[Sample])(rs: WrappedResultSet): Sample =
    apply(s.resultName)(rs)

  def apply(s: ResultName[Sample])(rs: WrappedResultSet): Sample =
    Sample(
      id = SampleId(rs.get[Long](s.id)),
      resourceId = ResourceId(rs.get[Long](s.resourceId)),
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

  //Por si a caso esto lo ordenaba por el nombre antes
//  override def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]] =
//    Future {
//      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)
//
//      DB.localTx { implicit session =>
//        withSQL {
//          selectFrom(this as s)
//            .append(
//              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
//            )
//            .orderBy(orderBy: _*)
//            .append(
//              ScalikeUtil.pag(pag)
//            )
//        }.map(apply(s.resultName)(_)).list()
//      }
//    }

  //Este se hace para ordenarlo por fecha de creación por defecto del mas nuevo al mas viejo
  override def find(filter: SampleFilter, pag: Pagination): Future[List[Sample]] =
    Future {
      val r = resourceMySqlRepository.syntax("r")
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          select(s.result.*)
            .from(this as s)
            .innerJoin(resourceMySqlRepository as r)
            .on(s.resourceId, r.id)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(orderBy: _*)
            .append(ScalikeUtil.pag(pag))
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
      val r = resourceMySqlRepository.syntax("r")
      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as r)
          .where
          .eq(r.id, s.resourceId)
          .and
          .eq(r.name, name)
          .toSQLSyntax
      )
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

    val userFilter = filter.userId.map { userId =>
      val ru = resourceUserMySqlRepository.syntax("ru")
      sqls.exists(
        select(sqls"1")
          .from(resourceUserMySqlRepository as ru)
          .where
          .eq(ru.resourceId, s.resourceId)
          .and
          .eq(ru.userId, userId.id)
          .and
          .eq(ru.deleted, false)
          .toSQLSyntax
      )
    }

    val filters = List(
      nameFilter,
      sampleTypeFilter,
      rockTypeFilter,
      isFreshFilter,
      userFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  private def buildOrderBy(pag: Pagination): Seq[SQLSyntax] = {
    val r = resourceMySqlRepository.syntax("r")

    val defaultOrderBy = Seq(r.created.desc, s.id.desc)

    pag.orderBy match {
      case Nil =>
        defaultOrderBy

      case seq =>
        seq.flatMap { field =>
          val desc = field.startsWith("-")

          field.stripPrefix("-") match {
//            case Strings.name =>
//              Some(if (desc) s.name.desc else s.name.asc)

            case Strings.created =>
              Some(if (desc) r.created.desc else r.created.asc)

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
