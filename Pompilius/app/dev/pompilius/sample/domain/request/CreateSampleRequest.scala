package dev.pompilius.sample.domain.request

import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

case class CreateSampleRequest(
    // Datos comunes (Resource)
    name: String,
    visibility: Visibility,
    location: String,
    observations: Option[String],
    summary: Option[String],
    price: Option[BigDecimal],
    isBarter: Boolean=false,

    // Datos específicos (Sample)

    collectedDate: DateTime,
    minerals: Option[String],
    collectionMethods: Option[String],
    isFresh: Boolean,
    sampleType: Option[String],
    materialsUsed: Option[String],
    sampleCategory: Option[String],
    geologicalProcesses: Option[String]
)

