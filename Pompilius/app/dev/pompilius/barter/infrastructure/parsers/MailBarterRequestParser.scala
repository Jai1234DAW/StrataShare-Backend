package dev.pompilius.barter.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.barter.domain.request.MailBarterRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.mvc.{AnyContentAsJson, Request}

object MailBarterRequestParser {
  def parse[A](request: Request[A]): MailBarterRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        MailBarterRequest(
          token = (json \ Strings.token).as[String](ReadsUtil.signedToken),
          transactionId = (json \ Strings.transactionId).as[String],
          email = (json \ Strings.email).as[String](ReadsUtil.email),
          barterId = (json \ Strings.barterId).as[String]
        )
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
