package dev.pompilius.shared.infrastructure

import org.joda.time.{DateTime, DateTimeZone, YearMonth}
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration

object JsUtils {

  def toJsValueWrapper[T](field: (String, T))(implicit w: Writes[T]): Option[(String, JsValueWrapper)] = {
    field match {
      case (_, None) =>
        None
      case _ =>
        Some(field._1, Json.toJsFieldJsValueWrapper(field._2))
    }
  }
  // DateTime
  implicit val JodaDateTimeReads: Reads[DateTime] = Reads[DateTime] { js =>
    js.validate[String].map[DateTime] { dtString =>
      DateTime.parse(dtString).withZone(DateTimeZone.UTC)
    }
  }

  implicit object JodaDateTimeWrites extends Writes[DateTime] {
    def writes(d: DateTime): JsValue = JsString(d.withZone(DateTimeZone.UTC).toString)
  }

  implicit val JodaDateTimeFormat: Format[DateTime] = Format(JodaDateTimeReads, JodaDateTimeWrites)

  // JodaMonthDate
  implicit val JodaYearMonthReads: Reads[YearMonth] = Reads[YearMonth] { js =>
    js.validate[JsObject].map[YearMonth] { yearMonth =>
      new YearMonth(
        (yearMonth \ "year").as[Int],
        (yearMonth \ "month").as[Int]
      )
    }
  }

  implicit object JodaYearMonthWrites extends Writes[YearMonth] {
    def writes(yearMonth: YearMonth): JsValue = {
      Json.obj(
        Seq(
          toJsValueWrapper("year", Some(yearMonth.getYear)),
          toJsValueWrapper("month", Some(yearMonth.getMonthOfYear))
        ).flatten: _*
      )
    }
  }

  implicit val JodaYearMonthFormat: Format[YearMonth] = Format(JodaYearMonthReads, JodaYearMonthWrites)

  // FiniteDuration
  implicit val FiniteDurationReads: Reads[FiniteDuration] = Reads[FiniteDuration] { js =>
    js.validate[JsObject].map[FiniteDuration] { finiteDuration =>
      FiniteDuration(
        length = (finiteDuration \ "length").as[Long],
        unit = (finiteDuration \ "unit").as[String]
      )
    }
  }

  implicit object FiniteDurationWrites extends Writes[FiniteDuration] {
    def writes(d: FiniteDuration): JsValue =
      Json.obj(
        Seq(
          toJsValueWrapper("length", Some(d.length)),
          toJsValueWrapper("unit", Some(d.unit.toString))
        ).flatten: _*
      )
  }

  implicit val FiniteDurationFormat: Format[FiniteDuration] = Format(FiniteDurationReads, FiniteDurationWrites)

  implicit val StringNumberReads: Reads[String] = Reads[String] {
    case JsString(s) =>
      JsSuccess(s)
    case JsNumber(s) =>
      JsSuccess(s.toString)
    case JsBoolean(s) =>
      JsSuccess(s.toString)
    case _ =>
      JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected String, Number or Boolean"))))
  }
}
