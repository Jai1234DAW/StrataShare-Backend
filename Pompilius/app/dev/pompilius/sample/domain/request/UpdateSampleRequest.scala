package dev.pompilius.sample.domain.request

import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

case class UpdateSampleRequest(
    // Datos comunes (Resource)
    name: Option[String] = None,
    visibility: Option[Visibility] = None,
    location: Option[String] = None,
    observations: Option[String] = None,
    summary: Option[String] = None,
    price: Option[BigDecimal] = None,
    isBarter: Option[Boolean] = None,
    // Datos específicos (Sample)
    collectedDate: Option[DateTime] = None,
    minerals: Option[String] = None,
    collectionMethods: Option[String] = None,
    isFresh: Option[Boolean] = None,
    sampleType: Option[String] = None,
    materialsUsed: Option[String] = None,
    sampleCategory: Option[String] = None,
    geologicalProcesses: Option[String] = None
)
