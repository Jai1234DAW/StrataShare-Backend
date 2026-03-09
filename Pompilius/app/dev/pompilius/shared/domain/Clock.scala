package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.infrastructure.HardwareClock
import org.joda.time.DateTime

@ImplementedBy(classOf[HardwareClock])
trait Clock {

  def now: DateTime

  def startOfDay: DateTime = now.withTimeAtStartOfDay()

  def endOfDay: DateTime = startOfDay.plusDays(1)

  def startOfWeek: DateTime = startOfDay.withDayOfWeek(1)

  def endOfWeek: DateTime = startOfWeek.plusWeeks(1)

  def startOfMonth: DateTime = startOfDay.withDayOfMonth(1)

  def endOfMonth: DateTime = startOfMonth.plusMonths(1)

  def startOfYear: DateTime = startOfDay.withDayOfYear(1)

  def endOfYear: DateTime = startOfYear.plusYears(1)

  def withoutSeconds: DateTime = now.withSecondOfMinute(0)

  def withoutMinutes: DateTime = withoutSeconds.withMinuteOfHour(0)

}