package dev.pompilius.resource.domain.sample

import dev.pompilius.resource.domain.ResourceId

case class Sample(
    id: SampleId,
    resourceId: ResourceId,
    name: String,
    minerals: Option[String],
    collectionMethods: Option[String],
    isFresh: Boolean,
    sampleType: Option[String],
    materialsUsed: Option[String],
    rockType: Option[String],
    geologicalProcesses: Option[String]
)
