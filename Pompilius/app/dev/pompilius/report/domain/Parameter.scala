package dev.pompilius.report.domain

case class Parameter(
    name: String,
    dataType: DataType,
    description: Option[String]
)
