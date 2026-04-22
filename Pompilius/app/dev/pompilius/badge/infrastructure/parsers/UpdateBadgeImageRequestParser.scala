package dev.pompilius.badge.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.badge.domain.request.UpdateBadgeImageRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}

object UpdateBadgeImageRequestParser {

  implicit val jsonReads: Reads[UpdateBadgeImageRequest] =
    (__ \ Strings.imageUrl).read[String](ReadsUtil.url).map(UpdateBadgeImageRequest.apply)

  def parse[A](request: Request[A]): UpdateBadgeImageRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[UpdateBadgeImageRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}

