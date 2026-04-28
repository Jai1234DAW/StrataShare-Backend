package dev.pompilius.sample.domain

import dev.pompilius.resource.domain.ResourceId
import org.joda.time.DateTime

case class Sample (
  // Campos específicos de Sample
  id: SampleId,
  resourceId: ResourceId,
  collectedDate: DateTime,
  minerals: Option[String] = None,
  collectionMethods: Option[String] = None,
  isFresh: Boolean = false,
  sampleType: Option[String] = None,
  materialsUsed: Option[String] = None,
  sampleCategory: Option[String] = None,
  geologicalProcesses: Option[String] = None
)
