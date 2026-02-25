package dev.pompilius.shared.infrastructure

import org.joda.time.{DateTime, DateTimeZone}
import dev.pompilius.shared.domain.Clock

import javax.inject.Singleton

@Singleton
class HardwareClock extends Clock {

  override def now: DateTime = DateTime.now.withMillisOfSecond(0).withZone(DateTimeZone.UTC)

}
