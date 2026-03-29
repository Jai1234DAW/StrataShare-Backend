package dev.pompilius.shared.infrastructure

import dev.pompilius.Strings
import play.api.libs.functional.syntax._
import play.api.libs.json._
import dev.pompilius.country.domain.Country

object ReadsUtil {

  // Valida un nombre de usuario: patrón regex, longitud mínima y máxima
  def username(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(
      Strings.usernameRegex,
      error = "error.username"
    ) keepAnd Reads.minLength[String](3) keepAnd Reads.maxLength[String](32)

  // Valida el nombre de una cuenta: longitud mínima y máxima
  def accountName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](64)

  // Valida el nombre de un widget: longitud mínima y máxima
  def widgetName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  // Valida el nombre de un juego: longitud mínima y máxima
  def gameName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  // Valida el título de un juego: longitud mínima y máxima
  def gameTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  // Valida la descripción de un juego: longitud mínima y máxima
  def gameDescription(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  // Valida el premio de un juego: longitud mínima y máxima
  def gamePrize(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](3) keepAnd Reads.maxLength[String](256)

  // Valida la contraseña: longitud máxima
  def password(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida el email: formato y longitud máxima
  def email(implicit reads: Reads[String]): Reads[String] =
    Reads.email keepAnd Reads.maxLength[String](256)

  // Valida que el string sea seguro para URL (patrón regex)
  def urlSafe(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(Strings.urlSafeRegex, "error.urlSafe")

  // Valida un alias: seguro para URL y longitud máxima
  def alias(implicit reads: Reads[String]): Reads[String] =
    urlSafe keepAnd Reads.maxLength[String](256)

  // Valida una tarifa: mínimo y máximo
  def fee(implicit reads: Reads[BigDecimal]): Reads[BigDecimal] =
    Reads.min(BigDecimal(0.01)) keepAnd Reads.max(BigDecimal(99.99))

  // Valida el teléfono: longitud máxima
  def phone(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  // Valida el primer nombre: longitud máxima
  def firstName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida el apellido: longitud máxima
  def lastName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida el número de documento: longitud máxima
  def documentNumber(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  // Valida el nombre completo: longitud máxima
  def fullName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida la cuenta de Stripe: longitud máxima
  def stripeAccount(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida la cuenta de PayPal: longitud máxima
  def paypalAccount(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida el nombre del banco: longitud máxima
  def bankName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida el titular de la cuenta bancaria: longitud máxima
  def bankAccountHolder(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida el número de cuenta bancaria: longitud máxima
  def bankAccountNumber(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida el BIC: longitud máxima
  def bic(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](16)

  // Valida la línea 1 de dirección: longitud máxima
  def addressLine1(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida la línea 2 de dirección: longitud máxima
  def addressLine2(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  // Valida la ciudad: longitud máxima
  def city(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida la provincia: longitud máxima
  def province(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida el código postal: longitud máxima
  def postalCode(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  // Valida el código de país: longitud exacta y existencia en la lista de países
  def countryCode(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2) keepAnd
      Reads.minLength[String](2) keepAnd
      Reads.filter[String](JsonValidationError("Invalid country code"))(
        Country.withNameInsensitiveOption(_).isDefined
      )

  // Valida el nombre de país: longitud máxima
  def country(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64)

  // Valida el idioma: longitud máxima
  def language(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2)

  // Valida el nombre de rol: longitud mínima y máxima
  def roleName(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](64) keepAnd Reads.minLength[String](1)

  // Valida un token firmado: patrón regex
  def signedToken(implicit reads: Reads[String]): Reads[String] =
    Reads.pattern(
      Strings.signedTokenRegex,
      error = "error.token"
    )

  //MIRAR AQUÍ, a partir de aquí puede que no lo use

  // Valida el nombre de libro: longitud mínima y máxima
  def bookName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida el nombre de región: longitud mínima y máxima
  def regionName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida el nombre de competición: longitud mínima y máxima
  def competitionName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida el nombre de equipo: longitud mínima y máxima
  def teamName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida el nombre de categoría: longitud mínima y máxima
  def categoryName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida el título de categoría: longitud mínima y máxima
  def categoryTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](64)

  // Valida una URL: longitud máxima
  def url(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2000)

  // Valida la descripción del premio de juego: longitud máxima
  def gamePrizeDescription(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  // Valida el recordatorio de juego: longitud máxima
  def gameRemainder(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  // Valida el título de acción de recordatorio de juego: longitud máxima
  def gameRemainderActionTitle(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](32)

  // Sample fields validations
  def sampleName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](256)

  def minerals(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  def collectionMethods(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  def sampleType(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def materialsUsed(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  def rockType(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def geologicalProcesses(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  // Study fields validations
  def studyName(implicit reads: Reads[String]): Reads[String] =
    Reads.minLength[String](1) keepAnd Reads.maxLength[String](256)

  def studyDescription(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](2000)

  def coordinates(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)

  def methods(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](1000)

  def authors(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](512)

  def nameSection(implicit reads: Reads[String]): Reads[String] =
    Reads.maxLength[String](256)
}
