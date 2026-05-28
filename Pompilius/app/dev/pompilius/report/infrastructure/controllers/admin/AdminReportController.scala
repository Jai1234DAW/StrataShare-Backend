package dev.pompilius.report.infrastructure.controllers.admin

import dev.pompilius.report.application.ReportFinder
import dev.pompilius.report.domain.{ReportFilter, ReportId, ReportRepository}
import dev.pompilius.report.infrastructure.writers.{ReportDataWriter, ReportWriter}
import dev.pompilius.shared.domain.{Clock, Paginated, Pagination, RequestLog, RequestLogger}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.users.domain.Role
import play.api.libs.Files.TemporaryFileCreator
import play.api.mvc.{Action, AnyContent}

import java.io.FileOutputStream
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AdminReportController @Inject() (
    reportFinder: ReportFinder,
    reportRepository: ReportRepository,
    paginatedWriter: PaginatedWriter,
    reportWriter: ReportWriter,
    reportDataWriter: ReportDataWriter,
    requestLogger: RequestLogger,
    temporaryFileCreator: TemporaryFileCreator,
    clock: Clock
)(implicit ec: ExecutionContext)
    extends BaseController {

  def getReports(
      name: Option[String],
      pag: Pagination
  ): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, user, _, _) =>
          for {
            // Buscamos los reportes
            reports <- reportRepository.find(
              filter = ReportFilter(
                name = name,
                userId = Some(user.id)
              ),
              pag = pag.oneMore
            )

            json <- paginatedWriter.toJson(Paginated(reports, pag)) { report => reportWriter.toJson(report) }

          } yield {
            Ok(json)
          }
      }
    }

  def getReport(reportId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, user, _, _) =>
          for {
            report <- reportFinder.getById(ReportId(reportId))
            reportJs <- reportWriter.toJson(report)

          } yield {
            Ok(reportJs)
          }
      }

    }

  def getReportResult(reportId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, user, _, _) =>
          for {
            report <- reportFinder.getById(ReportId(reportId))

            workbook <- reportDataWriter.toExcel(
              report = report,
              queryParameters = request.queryString.flatMap {
                case (key, values) =>
                  values.map(value => (key, value))
              }
            )

            _ <- requestLogger.log(
              RequestLog(
                id = dev.pompilius.shared.domain.RequestLogId.gen(configuration.nodeId),
                userId = user.id,
                timestamp = clock.now,
                address = request.remoteAddress,
                method = request.method,
                path = request.path,
                body = None,
                metadata = None
              )
            )
          } yield {
            val temporaryFile =
              temporaryFileCreator.create(report.name, ".xlsx")
            val fileOut = new FileOutputStream(temporaryFile)
            workbook.write(fileOut)
            fileOut.close()
            Ok.sendFile(content = temporaryFile, fileName = f => Some(f.getName), inline = false)
          }
      }
    }
}
