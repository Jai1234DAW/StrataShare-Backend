package dev.pompilius.shared.infrastructure

import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.xssf.usermodel.{XSSFWorkbook, _}
import org.joda.time.DateTime

import java.io.OutputStream

object ExcelWriter {

  case class Field(
      title: String,
      format: Option[String] = None,
      bold: Boolean = false,
      highlight: Boolean = false,
      autoSize: Boolean = false,
      width: Option[Int] = None
  )

  def newWorkBook = new XSSFWorkbook

  def write(
      stream: OutputStream,
      sheetName: String,
      fields: Seq[Field],
      rows: Seq[Seq[Any]]
  ): Unit = {
    val workbook = new XSSFWorkbook
    writeSheet(workbook, sheetName, fields, rows)
    workbook.write(stream)
  }

  def writeSheet(
      workbook: XSSFWorkbook,
      sheetName: String,
      fields: Seq[Field],
      rows: Seq[Seq[Any]]
  ): Unit = {
    val createHelper: XSSFCreationHelper = workbook.getCreationHelper

    val indexColorMap = new DefaultIndexedColorMap
    val headerStyle: XSSFCellStyle = workbook.createCellStyle()
    headerStyle.setFillForegroundColor(
      new XSSFColor(Array(0.toByte, 0.toByte, 0.toByte), indexColorMap)
    )
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND)

    val headerFont: XSSFFont = workbook.createFont()
    headerFont.setBold(true)
    headerFont.setColor(
      new XSSFColor(Array(255.toByte, 255.toByte, 255.toByte), indexColorMap)
    )
    headerStyle.setFont(headerFont)

    val boldFont: XSSFFont = workbook.createFont()
    boldFont.setBold(true)

    val sheet = workbook.createSheet(sheetName)
    val header = sheet.createRow(0)
    fields.zipWithIndex.foreach {
      case (field, index) =>
        val cell = header.createCell(index)
        cell.setCellValue(field.title)
        cell.setCellStyle(headerStyle)
    }

    val styles: Seq[XSSFCellStyle] = fields.map { field =>
      val style = workbook.createCellStyle()
      field.format.foreach(format => style.setDataFormat(createHelper.createDataFormat().getFormat(format)))
      if (field.bold) style.setFont(boldFont)
      if (field.highlight) {
        style.setFillForegroundColor(
          new XSSFColor(
            Array(255.toByte, 128.toByte, 128.toByte),
            indexColorMap
          )
        )
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND)
      }
      style
    }
    rows.zipWithIndex.foreach {
      case (rowValues, rowNum) =>
        val row = sheet.createRow(rowNum + 1)
        rowValues.zipWithIndex.zip(styles).foreach {
          case ((value, index), style) =>
            val cell = row.createCell(index)
            value match {
              case None                =>
              case v: Int              => cell.setCellValue(v)
              case v: Long             => cell.setCellValue(v.toDouble)
              case v: Double           => cell.setCellValue(v)
              case v: BigDecimal       => cell.setCellValue(v.toDouble)
              case v: String           => cell.setCellValue(v)
              case v: DateTime         => cell.setCellValue(v.toDate)
              case v: Boolean          => cell.setCellValue(v)
              case Some(v: Int)        => cell.setCellValue(v.toDouble)
              case Some(v: Long)       => cell.setCellValue(v.toDouble)
              case Some(v: Double)     => cell.setCellValue(v)
              case Some(v: BigDecimal) => cell.setCellValue(v.toDouble)
              case Some(v: String)     => cell.setCellValue(v)
              case Some(v: DateTime)   => cell.setCellValue(v.toDate)
              case Some(v: Boolean)    => cell.setCellValue(v)
              case _                   =>
            }
            cell.setCellStyle(style)
        }
    }

    fields.zipWithIndex.foreach {
      case (field, column) if field.autoSize =>
        sheet.autoSizeColumn(column)
      case _ =>
    }

    fields.zipWithIndex.foreach {
      case (field, column) =>
        field.width.foreach(width => sheet.setColumnWidth(column, width * 256))
    }
  }

}
