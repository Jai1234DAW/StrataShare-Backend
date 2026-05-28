package dev.pompilius.report.domain

import com.google.inject.ImplementedBy
import dev.pompilius.report.infrastructure.repositories.ReportMySqlRepository
import dev.pompilius.shared.domain.Pagination

import scala.concurrent.Future

@ImplementedBy(classOf[ReportMySqlRepository])
trait ReportRepository {
  def findById(id: ReportId): Future[Option[Report]]

  def find(filter: ReportFilter, pag: Pagination): Future[List[Report]]

  def getSheetRows(sheet: Sheet, parameters: Any*): Future[List[Row]]
}
