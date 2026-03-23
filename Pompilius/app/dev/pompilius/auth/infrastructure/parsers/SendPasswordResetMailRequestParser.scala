package dev.pompilius.auth.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.auth.domain.request.SendPasswordResetMailRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.mvc.{AnyContentAsJson, Request}

object SendPasswordResetMailRequestParser {

  def parse[A](request: Request[A]): SendPasswordResetMailRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        SendPasswordResetMailRequest(email = (json \ Strings.email).as[String](ReadsUtil.email))
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}
