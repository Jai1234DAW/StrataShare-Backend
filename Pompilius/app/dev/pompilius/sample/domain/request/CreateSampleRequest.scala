package dev.pompilius.sample.domain.request

import dev.pompilius.shared.domain.Visibility

case class CreateSampleRequest(
    // Datos comunes (Resource)
    name: String,
    visibility: Visibility,
    localization: String,
    observations: Option[String],
    summary: Option[String],

    // Datos específicos (Sample)

    minerals: Option[String],
    collectionMethods: Option[String],
    isFresh: Boolean,
    sampleType: Option[String],
    materialsUsed: Option[String],
    rockType: Option[String],
    geologicalProcesses: Option[String]
)

