package dev.pompilius.report.infrastructure.writers

import com.google.inject.ImplementedBy
import javax.inject.{Inject, Singleton}
import dev.pompilius.report.domain.{DataType, Report, ReportRepository, Sheet}
import dev.pompilius.shared.infrastructure.ExcelWriter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.joda.time.{DateTime, DateTimeZone}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ReportDataWriterImpl])
trait ReportDataWriter {
  def toExcel(report: Report, queryParameters: Map[String, String]): Future[XSSFWorkbook]
}

@Singleton
class ReportDataWriterImpl @Inject() (reportRepository: ReportRepository)(implicit val ec: ExecutionContext)
    extends ReportDataWriter {

  override def toExcel(report: Report, queryParameters: Map[String, String]): Future[XSSFWorkbook] = {
    val workbook = ExcelWriter.newWorkBook
    for {
      _ <- writeSheets(workbook = workbook, sheets = report.sheets, queryParameters = queryParameters)
    } yield {
      report.sheets.zipWithIndex.foreach { case (sheet, pos) => workbook.setSheetOrder(sheet.name, pos) }
      workbook
    }
  }

  private def writeSheets(
      workbook: XSSFWorkbook,
      sheets: Seq[Sheet],
      queryParameters: Map[String, String]
  ): Future[Unit] = {
    sheets match {
      case sheet :: tail =>
        writeSheet(
          workbook = workbook,
          sheet = sheet,
          queryParameters = queryParameters
        ).flatMap { _ =>
          writeSheets(workbook = workbook, sheets = tail, queryParameters = queryParameters)
        }
      case _ =>
        Future.unit
    }
  }

  private def writeSheet(workbook: XSSFWorkbook, sheet: Sheet, queryParameters: Map[String, String]): Future[Unit] = {

    val parameters = sheet.parameters.map { parameter =>
      queryParameters.get(parameter.name).map { value =>
        parameter.dataType match {
          case DataType.INT =>
            value.toInt
          case DataType.LONG =>
            value.toLong
          case DataType.DOUBLE =>
            value.toDouble
          case DataType.BIGDECIMAL =>
            BigDecimal(value)
          case DataType.STRING =>
            value
          case DataType.DATETIME =>
            DateTime.parse(value).withZone(DateTimeZone.UTC)
          case DataType.BOOLEAN =>
            value.toBoolean
        }
      }
    }

    for {
      rows <- reportRepository.getSheetRows(sheet, parameters: _*)
    } yield {
      ExcelWriter.writeSheet(
        workbook = workbook,
        sheetName = sheet.name,
        fields = sheet.columns.map { column =>
          ExcelWriter.Field(
            title = column.title.getOrElse(column.name),
            format = column.format,
            bold = column.bold,
            highlight = column.highlight,
            autoSize = column.autoSize,
            width = column.width
          )
        },
        rows = rows.map { row =>
          row.values.map {
            _.value
          }
        }
      )
    }
  }
}
