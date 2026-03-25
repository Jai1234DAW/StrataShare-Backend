package dev.pompilius.study.infrastructure.repositories

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.study.domain.exceptions.StudyNotFoundException
import dev.pompilius.study.domain.{Study, StudyFilter, StudyId, StudyRepository, Visibility, Area}
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
      visibility = Visibility.withName(rs.get[String](s.visibility)),
      localization = rs.get(s.localization),
      startDate = rs.get(s.startDate),
      endDate = rs.get(s.endDate),
      description = rs.get(s.description),
      coordinates = rs.get(s.coordinates),
      observations = rs.get(s.observations),
      summary = rs.get(s.summary),
      created = rs.get(s.created),
      updated = rs.get(s.updated),
      area = Area.withName(rs.get[String](s.area)),
      methods = rs.get(s.methods),
      authors = rs.get(s.authors),
      antecedent = rs.get(s.antecedent),
      section = rs.get(s.section),
      nameSection = rs.get(s.nameSection),
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
          selectFrom(this as s).where.like(s.name,name)
        }.map(apply(s.resultName)(_)).list()
      }
    }

  override def find(filter: StudyFilter, pag: Pagination): Future[List[Study]] =
    Future {
      val nameFilter: Option[SQLSyntax] = filter.name.map(name => sqls.eq(s.name, name))
      val visibilityFilter: Option[SQLSyntax] = filter.visibility.map(visibility => sqls.eq(s.visbility, visibility))
      val localizationFilter: Option[SQLSyntax] = filter.localization.map(localization => sqls.eq(u.localization, localiation))
      val startDateFilter: Option[SQLSyntax] = filter.startDate.map.(startDate =>
        sqls
          .roundBracket(
            sqls.gt(s.startDate, startDate).or.isNull(s.startDate)
          )
      )
      val endDateFilter: Option[SQLSyntax] = filter.endDate.map(endDate =>
        sqls
          .roundBracket(
            sqls.gt(s.endDate, endDate).or.isNull(s.endDate)
          )
      )
     val createdFilter: Option[SQLSyntax] = filter.created.val endDateFilter: Option[SQLSyntax] = filter.created.map(created=>
        sqls
          .roundBracket(
            sqls.gt(s.created, created).or.isNull(s.created)
          )
      )

      val areaFilter: Option[SQLSyntax] = filter.area.map(area => sqls.eq(s.area, area.toString))
      val methodsFilter: Option[SQLSyntax] = filter.methods.map(methods => sqls.eq(s.methods, methods))
     val authorsFilter: Option[SQLSyntax] = filter.authors.map(authors => sqls.eq(s.authors, authors))

      val searchFilter: Option[SQLSyntax] = filter.search.map(buildSearchFilter)

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as s)
            .append(
              List(
                nameFilter,
                visibilityFilter,
                localizationFilter,
                startDateFilter,
                endDateFilter,
                createdFilter,
                areaFilter,
                methodsFilter,
                authorsFilter
              ).flatten match {
                case l if l.nonEmpty =>
                  sqls.where.append(sqls.joinWithAnd(l: _*))
                case _ =>
                  sqls.empty
              }
            )
            .orderBy(s.id)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(s.resultName)(_)).list()
      }
    }

private def buildSearchFilter(search: String): SQLSyntax = {
  search.split(":") match {
    case Array(Strings.id, id) =>
      Try(StudyId(id.trim)).map(StudyId => sqls.eq(s.id, studyId.id)).getOrElse(defaultSearchFilter(search))
    case Array(Strings.name, name) =>
      val normalizedSearch = ("%" + name.trim + "%").replaceAll("( |%)+", "%")
      sqls.like(s.name, normalizedSearch)
    case Array(Strings.localization, localization) =>
      val normalizedSearch = ("%" + localization.trim + "%").replaceAll("( |%)+", "%")
      sqls.like(s.localization, normalizedSearch)
    case Array(Strings.authors, authors) =>
      val normalizedSearch = ("%" + authors.trim + "%").replaceAll("( |%)+", "%")
      sqls.like(s.authors, normalizedSearch)
    case Array(Strings.methods, methods) =>
      val normalizedSearch = ("%" + methods.trim + "%").replaceAll("( |%)+", "%")
      sqls.like(u.methods, normalizedSearch)
    case Array(Strings.area, area) =>
      sqls.eq(s.area, area.trim)
    case _ =>
      defaultSearchFilter(search)
  }
}

private def defaultSearchFilter(search: String): SQLSyntax = {
  val normalizedSearch = ("%" + search + "%").replaceAll("( |%)+", "%")
  sqls.roundBracket(
    sqls
      .like(u.username, normalizedSearch)
      .or
      .like(u.firstName, normalizedSearch)
      .or
      .like(u.lastName, normalizedSearch)
      .or
      .like(u.email, normalizedSearch)
      .or(Try(StudyId(search.trim)).toOption.map(id => sqls.eq(s.id, id.id)))
  )
}

  override def save(study: Study): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
              column.id -> study.id.id,
              column.name -> study.name,
              column.visibility -> study.visibility.value,
              column.localization -> study.localization,
              column.startDate -> study.startDate,
              column.endDate -> study.endDate,
              column.description -> study.description,
              column.coordinates -> study.coordinates,
              column.observations -> study.observations,
              column.summary -> study.summary,
              column.area -> study.area.entryName,
              column.methods -> study.methods,
              column.authors -> study.authors,
              column.antecedent -> study.antecedent,
              column.section -> study.section,
              column.nameSection -> study.nameSection,
              column.created -> study.created,
              column.updated -> study.updated
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

