package dev.pompilius.sample.domain.request

import dev.pompilius.shared.domain.Visibility

case class UpdateSampleRequest(
    // Datos comunes (Resource)
    visibility: Option[Visibility] = None,
    localization: Option[String] = None,
    observations: Option[String] = None,
    summary: Option[String] = None,
    // Datos específicos (Sample)
    name: Option[String] = None,
    minerals: Option[String] = None,
    collectionMethods: Option[String] = None,
    isFresh: Option[Boolean] = None,
    sampleType: Option[String] = None,
    materialsUsed: Option[String] = None,
    rockType: Option[String] = None,
    geologicalProcesses: Option[String] = None
)

