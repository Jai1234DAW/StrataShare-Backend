package dev.pompilius.sample.domain

import dev.pompilius.shared.domain.Visibility
import dev.pompilius.users.domain.UserId

case class SampleFilter(
    name: Option[String] = None,
    sampleType: Option[String] = None,
    rockType: Option[String] = None,
    isFresh: Option[Boolean] = None,
    visibility: Option[Visibility] = None,
    localization: Option[String] = None,
    userId: Option[UserId] = None
)
