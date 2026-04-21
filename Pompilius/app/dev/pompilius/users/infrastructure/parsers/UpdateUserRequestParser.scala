package dev.pompilius.users.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.users.domain.request.UpdateUserRequest
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.infrastructure.{ReadsUtil, StringUtil}
import play.api.i18n.Lang
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.shared.domain.exceptions.BadRequestException

object UpdateUserRequestParser {

  implicit val jsonReads: Reads[UpdateUserRequest] = (
    (__ \ Strings.username).read[String](ReadsUtil.username) and
      (__ \ Strings.interests).readNullable[List[String]] and
      (__ \ Strings.phone).readNullable[String](ReadsUtil.phone) and
      (__ \ Strings.firstName).readNullable[String](ReadsUtil.firstName) and
      (__ \ Strings.lastName).readNullable[String](ReadsUtil.lastName) and
      (__ \ Strings.country).read[String](ReadsUtil.countryCode).map(Country.withNameInsensitive) and
      (__ \ Strings.language).readNullable[String](ReadsUtil.language).map(_.map(lang => Lang(lang))) and
      (__ \ Strings.bio).readNullable[String].map(_.map(StringUtil.stripTags))
  )(UpdateUserRequest.apply _)

  def parse[A](request: Request[A]): UpdateUserRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[UpdateUserRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}
