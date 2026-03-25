package dev.pompilius.sample.domain

case class SampleFilter(
    name: Option[String] = None,
    sampleType: Option[String] = None,
    rockType: Option[String] = None,
    isFresh: Option[Boolean] = None,
    typeRock:Option[String] = None,
    localization: Option[String] = None
)

