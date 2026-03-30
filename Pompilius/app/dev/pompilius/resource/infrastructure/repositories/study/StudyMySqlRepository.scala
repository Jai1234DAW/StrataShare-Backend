package dev.pompilius.resource.infrastructure.repositories.study

import dev.pompilius.Strings
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.resource.domain.study._
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
class StudyMySqlRepository @Inject() (
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
      name = rs.get(st.name),
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

  override def find(filter: StudyFilter, pag: Pagination): Future[Seq[Study]] =
    Future {

      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as st)
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
    val defaultOrderBy = Seq(st.name, st.id.desc)

    pag.orderBy match {
      case Nil =>
        defaultOrderBy
      case seq =>
        seq.flatMap { field =>
          val desc = field.startsWith("-")
          field.stripPrefix("-") match {
            case Strings.name =>
              Some(if (desc) st.name.desc else st.name.asc)
            case _ =>
              None
          }
        } match {
          case Nil =>
            defaultOrderBy
          case e =>
            // Para asegurar un orden consistente, añadimos id al final
            e.appended(st.id.desc)
        }
    }
  }

  private def filterToSqlSyntax(filter: StudyFilter): Option[SQLSyntax] = {

    val searchFilter = filter.search.map { search =>
      val normalizedSearch = ScalikeUtil.normalizeSearch(search)
      sqls.roundBracket(
        sqls
          .like(st.name, normalizedSearch)
          .or
          .like(st.authors, normalizedSearch)
      )
    }

    val nameFilter = filter.name.map { name =>
      sqls.like(sqls.lower(st.name), s"%${name.toLowerCase}%")
    }

    val startToFilter = filter.startDate.map { sd =>
      sqls.ge(st.startDate, sd)
    }

    val endToFilter = filter.endDate.map { ed =>
      sqls.le(st.endDate, ed)
    }

    val areaFilter = filter.area.map { area =>
      sqls.eq(sqls.lower(st.area), area.entryName.toLowerCase)
    }

    val filters = List(
      nameFilter,
      startToFilter,
      endToFilter,
      areaFilter,
      searchFilter
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  override def save(study: Study): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> study.id.id,
          column.resourceId -> study.resourceId.id,
          column.name -> study.name,
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
        }.update()
      }
      Done
    }

  //Mirar si necesito esto mas adelante
//  override def findAllByUser(userId: UserId): Future[List[Study]] =
//    Future {
//      DB.localTx { implicit session =>
//        withSQL {
//          selectFrom(this as st).where
//            .eq(st.userId, userId.id)
//            .orderBy(st.createdAt.desc)
//        }.map(apply(st.resultName)(_)).list()
//      }
//    }
}
