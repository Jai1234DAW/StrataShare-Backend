package dev.pompilius.auth.infrastructure.parsers

import dev.pompilius.shared.infrastructure.ReadsUtil
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.auth.domain.request.PasswordResetRequest
import dev.pompilius.Strings
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}
import play.api.libs.functional.syntax._

object PasswordResetRequestParser {

  implicit val jsonReads: Reads[PasswordResetRequest] = (
    (__ \ Strings.newPassword).read[String](ReadsUtil.password) and
      (__ \ Strings.token).read[String](ReadsUtil.signedToken) and
      (__ \ Strings.closeAllSessions).read[Boolean]
  )(PasswordResetRequest.apply _)

  def parse[A](request: Request[A]): PasswordResetRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[PasswordResetRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}
