package dev.pompilius.auth.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.auth.domain.request.LoginRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.users.domain.UserPassword
import play.api.libs.functional.syntax._

object LoginRequestParser {

  implicit val jsonReads: Reads[LoginRequest] = (
    (__ \ Strings.username).read[String](ReadsUtil.username) and
      (__ \ Strings.password).read[String](ReadsUtil.password).map(UserPassword(_))
  )(LoginRequest.apply _)

  def parse[A](request: Request[A]): LoginRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[LoginRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
