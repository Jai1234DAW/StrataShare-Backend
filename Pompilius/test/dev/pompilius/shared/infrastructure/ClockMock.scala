package dev.pompilius.shared.infrastructure

import dev.pompilius.shared.domain.Clock
import org.scalatestplus.mockito.MockitoSugar
import org.joda.time.{DateTime, DateTimeZone, YearMonth}
import org.mockito.Mockito.when

trait ClockMock extends MockitoSugar {
  protected val clock: Clock= mock[Clock]

  clockShouldReturn(DateTime.now)

  protected def clockShouldReturn(dateTime: DateTime): Unit = {
    val now: DateTime = dateTime.withMillisOfSecond(0).withZone(DateTimeZone.UTC)
    val startOfDay: DateTime = now.withTimeAtStartOfDay()
    val startOfWeek: DateTime = startOfDay.withDayOfWeek(1)
    val startOfMonth: DateTime = startOfDay.withDayOfMonth(1)
    val startOfYear: DateTime = startOfDay.withDayOfYear(1)
    val withoutSeconds: DateTime = now.withSecondOfMinute(0)
    val yearMonth: YearMonth = new YearMonth(now)

    when(clock.now).thenReturn(now)
    when(clock.startOfDay).thenReturn(startOfDay)
    when(clock.endOfDay).thenReturn(startOfDay.plusDays(1))
    when(clock.startOfWeek).thenReturn(startOfWeek)
    when(clock.endOfWeek).thenReturn(startOfWeek.plusWeeks(1))
    when(clock.startOfMonth).thenReturn(startOfMonth)
    when(clock.endOfMonth).thenReturn(startOfMonth.plusMonths(1))
    when(clock.startOfYear).thenReturn(startOfYear)
    when(clock.endOfYear).thenReturn(startOfYear.plusYears(1))
    when(clock.withoutSeconds).thenReturn(withoutSeconds)
    when(clock.withoutMinutes).thenReturn(withoutSeconds.withMinuteOfHour(0))
    when(clock.yearMonth).thenReturn(yearMonth)
  }

}
