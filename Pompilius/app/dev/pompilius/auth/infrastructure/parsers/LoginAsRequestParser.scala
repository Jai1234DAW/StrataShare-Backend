package dev.pompilius.auth.infrastructure.parsers

import dev.pompilius.Strings
import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.auth.domain.request.LoginAsRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException

object LoginAsRequestParser {

  def parse[A](request: Request[A]): LoginAsRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        LoginAsRequest(username = (json \ Strings.username).as[String])
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }

}