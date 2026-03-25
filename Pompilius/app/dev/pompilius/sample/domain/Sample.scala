package dev.pompilius.sample.domain

import org.joda.time.DateTime

case class Sample(
    id: SampleId,
    name: String,
    description: Option[String],
    minerals: String,
    localization: String,
    collectionMethods: String,
    isFresh: Boolean,
    sampleType: String,
    materialsUsed: String,
    rockType: String,
    geologicalProcesses: String,
    created: DateTime,
    updated: DateTime
)

