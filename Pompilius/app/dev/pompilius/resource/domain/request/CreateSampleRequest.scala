package dev.pompilius.resource.domain.request

import dev.pompilius.shared.domain.Visibility
import play.api.libs.json.{Json, Reads}

case class CreateSampleRequest(
    visibility: Visibility,
    localization: String,
    observations: Option[String],
    summary: Option[String],
    name: String,
    minerals: Option[String],
    collectionMethods: Option[String],
    isFresh: Boolean,
    sampleType: Option[String],
    materialsUsed: Option[String],
    rockType: Option[String],
    geologicalProcesses: Option[String]
)

