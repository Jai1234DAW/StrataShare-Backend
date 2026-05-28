package dev.pompilius.report.domain

case class Column(
    name: String,
    title: Option[String],
    dataType: DataType,
    format: Option[String] = None,
    bold: Boolean = false,
    highlight: Boolean = false,
    autoSize: Boolean = false,
    width: Option[Int] = None
)
