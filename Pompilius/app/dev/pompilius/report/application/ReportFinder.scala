package dev.pompilius.report.application

import com.google.inject.ImplementedBy
import dev.pompilius.report.domain.exceptions.ReportNotFoundException
import dev.pompilius.report.domain.{Report, ReportId, ReportRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ReportFinderImpl])
trait ReportFinder {
  def getById(id: ReportId): Future[Report]
}

@Singleton
class ReportFinderImpl @Inject() (
    reportRepository: ReportRepository
)(implicit ec: ExecutionContext)
    extends ReportFinder {

  override def getById(id: ReportId): Future[Report] = {
    reportRepository.findById(id).map(_.getOrElse(throw ReportNotFoundException(id)))
  }

}
