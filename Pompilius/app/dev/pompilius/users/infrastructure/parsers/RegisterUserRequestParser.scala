package dev.pompilius.users.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import dev.pompilius.users.domain.request.RegisterUserRequest
import play.api.i18n.Lang
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}
import play.api.libs.functional.syntax._
import dev.pompilius.shared.infrastructure.StringUtil
import dev.pompilius.users.domain.Role

object RegisterUserRequestParser {

  implicit val jsonReads: Reads[RegisterUserRequest] = (
    (__ \ Strings.username).read[String](ReadsUtil.username) and
      (__ \ Strings.password).read[String] and
      (__ \ Strings.email).read[String](ReadsUtil.email) and
      (__ \ Strings.phone).readNullable[String](ReadsUtil.phone) and
      (__ \ Strings.firstName).readNullable[String](ReadsUtil.firstName) and
      (__ \ Strings.lastName).readNullable[String](ReadsUtil.lastName) and
      (__ \ Strings.country).read[String](ReadsUtil.countryCode).map(Country.withNameInsensitive) and
      (__ \ Strings.language).readNullable[String](ReadsUtil.language).map(_.map(lang => Lang(lang))) and
      (__ \ Strings.notes).readNullable[String] and
      (__ \ Strings.bio).readNullable[String].map(_.map(StringUtil.stripTags)) and
      (__ \ Strings.role).read[String].map(Role.withNameInsensitive)
  )(RegisterUserRequest.apply _)


  def parse[A](request: Request[A]): RegisterUserRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[RegisterUserRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}
