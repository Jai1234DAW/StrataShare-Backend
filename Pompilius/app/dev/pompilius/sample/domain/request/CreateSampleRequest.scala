package dev.pompilius.sample.domain.request

import dev.pompilius.shared.domain.Visibility

case class CreateSampleRequest(
    // Datos comunes (Resource)
    name: String,
    visibility: Visibility,
    location: String,
    observations: Option[String],
    summary: Option[String],
    price: Option[BigDecimal],
    isBarter: Boolean,

    // Datos específicos (Sample)

    minerals: Option[String],
    collectionMethods: Option[String],
    isFresh: Boolean,
    sampleType: Option[String],
    materialsUsed: Option[String],
    sampleCategory: Option[String],
    geologicalProcesses: Option[String]
)

