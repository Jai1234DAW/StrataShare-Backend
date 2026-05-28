package dev.pompilius.report.domain.exceptions

import dev.pompilius.report.domain.ReportId
import dev.pompilius.shared.domain.VerboseException

class ReportNotFoundException(message: String = "Report not found") extends VerboseException(message = message)

object ReportNotFoundException {
  def apply(reportId: ReportId): ReportNotFoundException = {
    new ReportNotFoundException(s"Report with id=${reportId.toString} not found")
  }
}
