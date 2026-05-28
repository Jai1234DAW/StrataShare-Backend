package dev.pompilius.review.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.review.domain.request.UpdateReviewRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object UpdateReviewRequestParser {
  implicit val jsonReads: Reads[UpdateReviewRequest] = (
    (__ \ Strings.rating).readNullable[Int](ReadsUtil.rating) and
      (__ \ Strings.comment).readNullable[String]
  )(UpdateReviewRequest.apply _)
  def parse[A](request: Request[A]): UpdateReviewRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[UpdateReviewRequest]
      case _ =>
        throw new BadRequestException("Expecting application/json body")
    }
  }
}
