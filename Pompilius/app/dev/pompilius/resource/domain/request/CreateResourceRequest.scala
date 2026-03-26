package dev.pompilius.resource.domain.request

case class CreateResourceRequest(
    resourceType: String,
    visibility: String,
    localization: String,
    observations: Option[String] = None,
    summary: Option[String] = None
)


