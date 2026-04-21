package dev.pompilius.review.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.review.domain.request.CreateReviewRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.ReadsUtil
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateReviewRequestParser {
  implicit val jsonReads: Reads[CreateReviewRequest] = (
    (__ \ Strings.resourceId).read[String] and
      (__ \ Strings.rating).read[Int](ReadsUtil.rating) and
      (__ \ Strings.comment).readNullable[String]
  )(CreateReviewRequest.apply _)
  def parse[A](request: Request[A]): CreateReviewRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateReviewRequest]
      case _ =>
        throw new BadRequestException("Expecting application/json body")
    }
  }
}
