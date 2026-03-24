package dev.pompilius.users.infrastructure.parsers

import play.api.mvc.{AnyContentAsJson, Request}
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.users.domain.request.SendMailChangeRequest
import dev.pompilius.Strings
import dev.pompilius.shared.infrastructure.ReadsUtil

//Mirar esto
object SendMailChangeRequestParser {

  def parse[A](request: Request[A]): SendMailChangeRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        SendMailChangeRequest(email = (json \ Strings.email).as[String](ReadsUtil.email))
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}