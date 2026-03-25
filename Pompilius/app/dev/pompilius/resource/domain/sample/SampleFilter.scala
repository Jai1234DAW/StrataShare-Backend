package dev.pompilius.resource.domain.sample

case class SampleFilter(
    name: Option[String] = None,
    sampleType: Option[String] = None,
    rockType: Option[String] = None,
    isFresh: Option[Boolean] = None,
)

