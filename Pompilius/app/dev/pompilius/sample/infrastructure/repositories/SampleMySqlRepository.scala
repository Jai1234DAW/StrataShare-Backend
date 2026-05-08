package dev.pompilius.sample.infrastructure.repositories

import dev.pompilius.Strings
import dev.pompilius.resource.domain.{ResourceId, ResourceUserType}
import dev.pompilius.resource.infrastructure.repositories.{ResourceMySqlRepository, ResourceUserMySqlRepository}
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

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
      collectedDate = rs.get(s.collectedDate),
      minerals = rs.get(s.minerals),
      collectionMethods = rs.get(s.collectionMethods),
      isFresh = rs.get(s.isFresh),
      sampleType = rs.get(s.sampleType),
      materialsUsed = rs.get(s.materialsUsed),
      sampleCategory = rs.get(s.sampleCategory),
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
          selectFrom(this as s)
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

  override def getMyAllSamplesAs(
      userId: UserId,
      pag: Pagination, userType: String
  ): Future[List[Sample]] =
    Future {
      val r = resourceMySqlRepository.syntax("r")
      val ru = resourceUserMySqlRepository.syntax("ru")
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          select(s.result.*)
            .from(this as s)
            .innerJoin(resourceMySqlRepository as r)
            .on(s.resourceId, r.id)
            .innerJoin(resourceUserMySqlRepository as ru)
            .on(r.id, ru.resourceId)
            .where
            .eq(ru.userId, userId.id)
            .and
            .eq(ru.deleted, false)
            .and
            .eq(ru.resourceUserType, userType)
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
      val re = resourceMySqlRepository.syntax("re")
      val value = s"%${name.trim.toLowerCase}%"

      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as re)
          .where
          .eq(re.id, s.resourceId)
          .and
          .like(sqls"LOWER(TRIM(${re.name}))", value)
          .toSQLSyntax
      )
    }

    val sampleTypeFilter = filter.sampleType.map { sampleType =>
      sqls.like(sqls.lower(s.sampleType), ScalikeUtil.normalizeSearch(sampleType.toLowerCase))
    }

    val sampleCategoryFilter = filter.sampleCategory.map { sampleCategory =>
      sqls.like(sqls.lower(s.sampleCategory), ScalikeUtil.normalizeSearch(sampleCategory.toLowerCase))
    }

    val isFreshFilter = filter.isFresh.map { isFresh =>
      sqls.eq(s.isFresh, isFresh)
    }

    val locationFilter = filter.location.map { location =>
      val r = resourceMySqlRepository.syntax("r")
      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as r)
          .where
          .eq(r.id, s.resourceId)
          .and
          .like(sqls.lower(r.location), ScalikeUtil.normalizeSearch(location.toLowerCase))
          .toSQLSyntax
      )
    }

    val visibilityFilter = filter.visibility.map { visibility =>
      val r = resourceMySqlRepository.syntax("r")
      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as r)
          .where
          .eq(r.id, s.resourceId)
          .and
          .eq(r.visibility, visibility.toString)
          .toSQLSyntax
      )
    }

    val searchFilter = filter.search.map { search =>
      val r = resourceMySqlRepository.syntax("r")
      val value = ScalikeUtil.normalizeSearch(search.toLowerCase)

      sqls.roundBracket(
        SQLSyntax.joinWithOr(
          Seq(
            sqls.exists(
              select(sqls"1")
                .from(resourceMySqlRepository as r)
                .where
                .eq(r.id, s.resourceId)
                .and
                .like(sqls.lower(r.name), value)
                .toSQLSyntax
            ),
            sqls.exists(
              select(sqls"1")
                .from(resourceMySqlRepository as r)
                .where
                .eq(r.id, s.resourceId)
                .and
                .like(sqls.lower(r.location), value)
                .toSQLSyntax
            ),
            sqls.like(sqls.lower(s.sampleCategory), value)
          ):_*
        )
      )
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
          .and
          .eq(ru.resourceUserType, ResourceUserType.OWNER.toString)
          .toSQLSyntax
      )
    }

    val resourceUserNotDeletedFilter = {
      val ru = resourceUserMySqlRepository.syntax("ru")

      sqls.exists(
        select(sqls"1")
          .from(resourceUserMySqlRepository as ru)
          .where
          .eq(ru.resourceId, s.resourceId)
          .and
          .eq(ru.resourceUserType, ResourceUserType.OWNER.toString)
          .and
          .eq(ru.deleted, false)
          .toSQLSyntax
      )
    }

    val filters = List(
      nameFilter,
      sampleTypeFilter,
      sampleCategoryFilter,
      isFreshFilter,
      visibilityFilter,
      locationFilter,
      searchFilter,
      Some(resourceUserNotDeletedFilter),
      userFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  private def buildOrderBy(pag: Pagination): Seq[SQLSyntax] = {
    val r = resourceMySqlRepository.syntax("r")

    val defaultOrderBy = Seq(r.created.desc,  r.name.asc, s.id.desc)

    pag.orderBy match {
      case Nil =>
        defaultOrderBy

      case seq =>
        val orderBy = seq.flatMap { field =>
          val desc = field.startsWith("-")
          val cleanField = field.stripPrefix("-")

          cleanField match {
            case Strings.created =>
              Some(if (desc) r.created.desc else r.created.asc)

            case Strings.name =>
              Some(if (desc) r.name.desc else r.name.asc)

            case _ =>
              None
          }
        }

        orderBy match {
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
          column.collectedDate -> sample.collectedDate,
          column.minerals -> sample.minerals,
          column.collectionMethods -> sample.collectionMethods,
          column.isFresh -> sample.isFresh,
          column.sampleType -> sample.sampleType,
          column.materialsUsed -> sample.materialsUsed,
          column.sampleCategory -> sample.sampleCategory,
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
