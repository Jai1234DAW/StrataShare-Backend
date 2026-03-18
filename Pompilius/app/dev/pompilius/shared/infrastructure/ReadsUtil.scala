package dev.pompilius.shared.infrastructure

import dev.pompilius.Strings
import dev.pompilius.country.domain.Country
import play.api.libs.functional.syntax._
import play.api.libs.json._

object ReadsUtil {

  def username(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(
      Strings.usernameRegex,
      error = "error.username"
    ) keepAnd Reads.minLength[String](3) keepAnd Reads.maxLength[String](32)

  def accountName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](64)

  def widgetName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  def gameName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  def gameTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  def gameDescription(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  def gamePrize(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  def password(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def email(implicit reads: Reads[String]): Reads[String] =
    Reads.email keepAnd Reads.maxLength[String](256)

  def urlSafe(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(Strings.urlSafeRegex, "error.urlSafe")

  def alias(implicit reads: Reads[String]): Reads[String] =
    urlSafe keepAnd Reads.maxLength[String](256)

  def fee(implicit reads: Reads[BigDecimal]): Reads[BigDecimal] =
    Reads.min(BigDecimal(0.01)) keepAnd Reads.max(BigDecimal(99.99))

  def phone(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  def firstName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def lastName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def documentNumber(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  def fullName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def stripeAccount(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def paypalAccount(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def bankName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def bankAccountHolder(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def bankAccountNumber(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def bic(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](16)

  def addressLine1(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def addressLine2(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def city(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def province(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def postalCode(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  def countryCode(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2) keepAnd
      Reads.minLength[String](2) keepAnd
      Reads.filter[String](JsonValidationError("Invalid country code"))(
        Country.withNameInsensitiveOption(_).isDefined
      )

  def country(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  def language(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2)

  def roleName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64) keepAnd Reads.minLength[String](1)

  def signedToken(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(
      Strings.signedTokenRegex,
      error = "error.token"
    )

  def bookName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def regionName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def competitionName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def teamName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def categoryName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def categoryTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  def url(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2000)

  def gamePrizeDescription(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)
  def gameRemainder(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)
  def gameRemainderActionTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)
}
