package dev.pompilius.sample.domain

import dev.pompilius.resource.domain.ResourceId

case class Sample (
  // Campos específicos de Sample
  id: SampleId,
  resourceId: ResourceId,
  name: String,
  minerals: Option[String] = None,
  collectionMethods: Option[String] = None,
  isFresh: Boolean = false,
  sampleType: Option[String] = None,
  materialsUsed: Option[String] = None,
  rockType: Option[String] = None,
  geologicalProcesses: Option[String] = None
)
