package dev.pompilius.sample.domain

import dev.pompilius.users.domain.UserId

case class SampleFilter(
    name: Option[String] = None,
    sampleType: Option[String] = None,
    rockType: Option[String] = None,
    isFresh: Option[Boolean] = None,
    userId: Option[UserId] = None
)
