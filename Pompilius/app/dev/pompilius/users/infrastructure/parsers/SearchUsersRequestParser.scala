package dev.pompilius.users.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.users.domain.request.SearchUsersRequest
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.infrastructure.ReadsUtil
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}

object SearchUsersRequestParser {

  implicit val jsonReads: Reads[SearchUsersRequest] = (
    (__ \ Strings.username).readNullable[String] and
      (__ \ Strings.firstName).readNullable[String] and
      (__ \ Strings.lastName).readNullable[String] and
      (__ \ Strings.country).readNullable[String](ReadsUtil.countryCode).map(_.map(Country.withNameInsensitive)) and
      (__ \ Strings.search).readNullable[String] and
      (__ \ Strings.limit).readNullable[Int] and
      (__ \ Strings.offset).read[Int](Reads.IntReads).orElse(Reads.pure(0))
  )(SearchUsersRequest.apply _)

  def parse[A](request: Request[A]): SearchUsersRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[SearchUsersRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}

