package dev.pompilius.users.infrastructure.parsers

import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import dev.pompilius.users.domain.request.ChangeUserPasswordRequest
import dev.pompilius.Strings
import play.api.libs.functional.syntax._

object ChangeUserPasswordRequestParser {

  implicit val jsonReads: Reads[ChangeUserPasswordRequest] = (
    (__ \ Strings.oldPassword).read[String](ReadsUtil.password) and
      (__ \ Strings.newPassword).read[String](ReadsUtil.password) and
      (__ \ Strings.closeAllSessions).read[Boolean]
  )(ChangeUserPasswordRequest.apply _)

  def parse[A](request: Request[A]): ChangeUserPasswordRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[ChangeUserPasswordRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
