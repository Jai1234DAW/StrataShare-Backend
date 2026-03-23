package dev.pompilius.users.infrastructure.parsers

import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.users.domain.request.ChangeMailRequest
import dev.pompilius.Strings
import dev.pompilius.shared.infrastructure.ReadsUtil

object ChangeMailRequestParser {

  def parse[A](request: Request[A]): ChangeMailRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        ChangeMailRequest(
          email = (json \ Strings.email).as[String](ReadsUtil.email),
          token = (json \ Strings.token).as[String](ReadsUtil.signedToken)
        )
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}

