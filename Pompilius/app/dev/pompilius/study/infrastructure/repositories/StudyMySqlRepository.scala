package dev.pompilius.study.infrastructure.repositories

import dev.pompilius.Strings
import dev.pompilius.resource.domain.{ResourceId, ResourceUserType}
import dev.pompilius.resource.infrastructure.repositories.{ResourceMySqlRepository, ResourceUserMySqlRepository}
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.study.domain.{Area, Study, StudyFilter, StudyId, StudyRepository}
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import org.joda.time.DateTime
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StudyMySqlRepository @Inject() (
    resourceUserMySqlRepository: ResourceUserMySqlRepository,
    resourceMySqlRepository: ResourceMySqlRepository
)(implicit dbExecutionContext: DbExecutionContext)
    extends StudyRepository
    with SQLSyntaxSupport[Study] {

  override val tableName = "study"

  def apply(st: SyntaxProvider[Study])(rs: WrappedResultSet): Study =
    apply(st.resultName)(rs)

  def apply(st: ResultName[Study])(rs: WrappedResultSet): Study =
    Study(
      id = StudyId(rs.get[Long](st.id)),
      resourceId = ResourceId(rs.get[Long](st.resourceId)),
      startDate = rs.get(st.startDate),
      endDate = rs.get(st.endDate),
      description = rs.get(st.description),
      coordinates = rs.get(st.coordinates),
      area = Area.withNameInsensitive(rs.get[String](st.area)),
      methods = rs.get(st.methods),
      authors = rs.get(st.authors),
      section = rs.get(st.section),
      antecedents = rs.get(st.antecedents),
      nameSection = rs.get[Option[String]](st.nameSection)
    )

  private val st = this.syntax("st")

  override def findById(id: StudyId): Future[Option[Study]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as st).where.eq(st.id, id.id)
        }.map(apply(st.resultName)(_)).single()
      }
    }

  override def find(filter: StudyFilter, pag: Pagination): Future[List[Study]] =
    Future {
      val r = resourceMySqlRepository.syntax("r")
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          select(st.result.*)
            .from(this as st)
            .innerJoin(resourceMySqlRepository as r)
            .on(st.resourceId, r.id)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(orderBy: _*)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(st.resultName)(_)).list()
      }
    }

  private def buildOrderBy(pag: Pagination): Seq[SQLSyntax] = {
    val r = resourceMySqlRepository.syntax("r")

    val defaultOrderBy = Seq(r.created.desc, r.name.asc, st.id.desc)

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
              Some(if (desc) r.name.asc else r.name.desc)

            case _ =>
              None
          }
        }

        orderBy match {
          case Nil =>
            defaultOrderBy

          case e =>
            e.appended(st.id.desc)
        }
    }
  }

  private def filterToSqlSyntax(filter: StudyFilter): Option[SQLSyntax] = {

    val searchFilter = filter.search.map { search =>
      val re = resourceMySqlRepository.syntax("re")
      val value = ScalikeUtil.normalizeSearch(search.toLowerCase)

      sqls.roundBracket(
        sqls
          .like(sqls.lower(st.description), value)
          .or
          .like(sqls.lower(st.authors), value)
          .or
          .like(sqls.lower(st.area), value)
          .or
          .exists(
            select(sqls"1")
              .from(resourceMySqlRepository as re)
              .where
              .eq(re.id, st.resourceId)
              .and
              .like(sqls.lower(re.name), value)
              .toSQLSyntax
          )
      )
    }

    val nameFilter = filter.name.map { name =>
      val re = resourceMySqlRepository.syntax("re")
      val value = s"%${name.trim.toLowerCase}%"

      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as re)
          .where
          .eq(re.id, st.resourceId)
          .and
          .like(sqls"LOWER(TRIM(${re.name}))", value)
          .toSQLSyntax
      )
    }

    val yearFilter = filter.year.map { year =>
      val startOfYear = new DateTime(year, 1, 1, 0, 0)
      val endOfYear = startOfYear.plusYears(1).minusMillis(1)

      // Filtra proyectos que comiencen o terminen en ese año
      sqls.roundBracket(
        sqls
          .between(st.startDate, startOfYear, endOfYear)
          .or
          .between(st.endDate, startOfYear, endOfYear)
      )
    }

    val areaFilter = filter.area.map { area =>
      sqls.eq(sqls.lower(st.area), area.entryName.toLowerCase)
    }

    val authorsFilter = filter.authors.map { authors =>
      sqls.like(
        sqls.lower(st.authors),
        ScalikeUtil.normalizeSearch(authors.toLowerCase)
      )
    }

    val userFilter = filter.userId.map { userId =>
      val ru = resourceUserMySqlRepository.syntax("ru")
      sqls.exists(
        select(sqls"1")
          .from(resourceUserMySqlRepository as ru)
          .where
          .eq(ru.resourceId, st.resourceId)
          .and
          .eq(ru.userId, userId.id)
          .and
          .eq(ru.deleted, false)
          .toSQLSyntax
      )
    }

    val visibilityFilter = filter.visibility.map { visibility =>
      val r = resourceMySqlRepository.syntax("r")
      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as r)
          .where
          .eq(r.id, st.resourceId)
          .and
          .eq(r.visibility, visibility.entryName)
          .toSQLSyntax
      )
    }

    val locationFilter = filter.location.map { location =>
      val r = resourceMySqlRepository.syntax("r")
      sqls.exists(
        select(sqls"1")
          .from(resourceMySqlRepository as r)
          .where
          .eq(r.id, st.resourceId)
          .and
          .like(sqls.lower(r.location), ScalikeUtil.normalizeSearch(location.toLowerCase))
          .toSQLSyntax
      )
    }

//      sqls.in(
//        st.resourceId,
//        select(ru.resourceId)
//          .from(resourceUserMySqlRepository as ru)
//          .where
//          .eq(ru.userId, userId.id)
//          .and
//          .eq(ru.deleted, false)
//          .toSQLSyntax
//      )

    val resourceUserNotDeletedFilter = {
      val ru = resourceUserMySqlRepository.syntax("ru")

      sqls.exists(
        select(sqls"1")
          .from(resourceUserMySqlRepository as ru)
          .where
          .eq(ru.resourceId, st.resourceId)
          .and
          .eq(ru.resourceUserType, ResourceUserType.OWNER.toString)
          .and
          .eq(ru.deleted, false)
          .toSQLSyntax
      )
    }

    val filters = List(
      Some(resourceUserNotDeletedFilter),
      nameFilter,
      yearFilter,
      areaFilter,
      authorsFilter,
      searchFilter,
      userFilter,
      visibilityFilter,
      locationFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def getMyAllStudiesAs(
      userId: UserId,
      pag: Pagination, resourceUserType: String
  ): Future[List[Study]] =
    Future {
      val r = resourceMySqlRepository.syntax("r")
      val ru = resourceUserMySqlRepository.syntax("ru")
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          select(st.result.*)
            .from(this as st)
            .innerJoin(resourceMySqlRepository as r)
            .on(st.resourceId, r.id)
            .innerJoin(resourceUserMySqlRepository as ru)
            .on(r.id, ru.resourceId)
            .where
            .eq(ru.userId, userId.id)
            .and
            .eq(ru.resourceUserType, resourceUserType)
            .and
            .eq(ru.deleted, false)
            .orderBy(orderBy: _*)
            .append(ScalikeUtil.pag(pag))

        }.map(apply(st.resultName)(_)).list()
      }
    }

  override def save(study: Study): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> study.id.id,
          column.resourceId -> study.resourceId.id,
          column.startDate -> study.startDate,
          column.endDate -> study.endDate,
          column.description -> study.description,
          column.coordinates -> study.coordinates,
          column.area -> study.area.entryName,
          column.methods -> study.methods,
          column.authors -> study.authors,
          column.section -> study.section,
          column.antecedents -> study.antecedents,
          column.nameSection -> study.nameSection
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

  override def findByResource(resourceId: ResourceId): Future[Option[Study]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as st).where.eq(st.resourceId, resourceId.id)
        }.map(apply(st.resultName)(_)).single()
      }
    }

  override def delete(id: StudyId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(st.id, id.id)
        }.update()
      }
      Done
    }

  //Funciona pero trabajaremos con el de arriba
//  override def findAllByUser(userId: UserId): Future[List[Study]] =
//    Future {
//      DB.localTx { implicit session =>
//        val ru = resourceUserMySqlRepository.syntax("ru")
//
//        withSQL {
//          select(st.result.*)
//            .from(this as st)
//            .innerJoin(resourceUserMySqlRepository as ru)
//            .on(st.resourceId, ru.resourceId)
//            .where
//            .eq(ru.userId, userId.id)
//            .and
//            .eq(ru.deleted, false)
//            .orderBy(st.name.desc)
//        }.map(apply(st.resultName)(_)).list()
//      }
//    }
}
