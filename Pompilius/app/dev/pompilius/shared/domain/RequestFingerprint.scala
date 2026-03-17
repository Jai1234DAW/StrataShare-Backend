package dev.pompilius.shared.domain

import dev.pompilius.country.domain.Country
import play.api.i18n.Lang
import scala.util.Try

case class RequestFingerprint(
    remoteAddress: String,
    country: Option[Country],
    userAgent: Option[String],
    language: Option[String]
) {
  def getLang: Option[Lang] = language.flatMap(_.split(',').headOption).flatMap(lang => Try(Lang(lang)).toOption)
}
