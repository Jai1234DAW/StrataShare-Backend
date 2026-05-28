package dev.pompilius.report.infrastructure.repositories

import dev.pompilius.report.domain._
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}
import scalikejdbc._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ReportMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext) extends ReportRepository with SQLSyntaxSupport[Report] {

  override val tableName = "report"

  implicit val reportColumnJsFormat: Format[Column] = Json.format[Column]
  implicit val reportParameterJsFormat: Format[Parameter] = Json.format[Parameter]
  implicit val reportSheetJsFormat: Format[Sheet] = Json.format[Sheet]

  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))


  def apply(re: SyntaxProvider[Report])(rs: WrappedResultSet): Report =
    apply(re.resultName)(rs)
  def apply(re: ResultName[Report])(rs: WrappedResultSet): Report =
    Report(
      id = ReportId(rs.get[Long](re.id)),
      name = rs.get(re.name),
      title = rs.get(re.title),
      authorizedUsers = Json.parse(rs.get[String](re.authorizedUsers)).as[List[Long]].map(UserId(_)),
      sheets = Json.parse(rs.get[String](re.sheets)).as[Seq[Sheet]]
    )

  private val re = this.syntax("re")

  override def findById(id: ReportId): Future[Option[Report]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as re).where.eq(re.id, id.id)
        }.map(apply(re.resultName)(_)).single()
      }
    }

  override def find(filter: ReportFilter, pag: Pagination): Future[List[Report]] =
    Future {

      val nameFilter: Option[SQLSyntax] = filter.name.map { name =>
        val normalizedName = ("%" + name + "%").replaceAll("( |%)+", "%")
        sqls.like(re.name, normalizedName)
      }

      val userFilter: Option[SQLSyntax] = filter.userId.map { userId =>
        ScalikeUtil.jsonContains(re.authorizedUsers, userId.id.toString)
      }

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as re)
            .append(
              List(nameFilter, userFilter).flatten match {
                case l if l.nonEmpty =>
                  sqls.where.append(sqls.joinWithAnd(l: _*))
                case _ =>
                  sqls.empty
              }
            )
            .orderBy(re.id)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(re.resultName)(_)).list()
      }
    }

  override def getSheetRows(sheet: Sheet, parameters: Any*): Future[List[Row]] =
    Future {

      DB.localTx { implicit session =>
        val query = if (parameters.nonEmpty) SQL(sheet.query).bind(parameters: _*) else SQL(sheet.query)

        query
          .map { rs =>
            Row(
              sheet.columns.map { column =>
                val value = column.dataType match {
                  case DataType.INT =>
                    rs.intOpt(column.name)
                  case DataType.LONG =>
                    rs.longOpt(column.name)
                  case DataType.DOUBLE =>
                    rs.doubleOpt(column.name)
                  case DataType.BIGDECIMAL =>
                    rs.get[Option[BigDecimal]](column.name)
                  case DataType.STRING =>
                    rs.stringOpt(column.name)
                  case DataType.DATETIME =>
                    rs.get[Option[DateTime]](column.name)
                  case DataType.BOOLEAN =>
                    rs.booleanOpt(column.name)
                }

                FieldValue(
                  dataType = column.dataType,
                  value = value
                )
              }
            )
          }
          .list()

      }
    }

}


