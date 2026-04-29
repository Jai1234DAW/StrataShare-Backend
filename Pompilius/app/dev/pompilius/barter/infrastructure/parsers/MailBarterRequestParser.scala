package dev.pompilius.barter.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.barter.domain.request.AcceptMailBarterRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.mvc.{AnyContentAsJson, Request}

object AcceptMailBarterRequestParser {
  def parse[A](request: Request[A]): AcceptMailBarterRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        AcceptMailBarterRequest(
          token = (json \ Strings.token).as[String](ReadsUtil.signedToken),
          transactionId = (json \ Strings.transactionId).as[String],
          email = (json \ Strings.email).as[String](ReadsUtil.email)
        )
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
