package dev.pompilius.report.domain

case class Sheet(
    name: String,
    query: String,
    columns: Seq[Column],
    parameters: Seq[Parameter]
)
