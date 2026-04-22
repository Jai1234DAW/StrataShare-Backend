package dev.pompilius.badge.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.mvc.Request

case class UpdateBadgeImageRequest(imageUrl: String)

object UpdateBadgeImageRequestParser {

  def parse(implicit request: Request[_]): UpdateBadgeImageRequest = {
    val json = request.body.asJson.getOrElse(throw new BadRequestException("Invalid JSON"))

    val imageUrl = (json \ "imageUrl")
      .asOpt[String]
      .filter(_.trim.nonEmpty)
      .getOrElse(throw new BadRequestException("imageUrl is required and cannot be empty"))

    // Validación básica de URL
    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
      throw new BadRequestException("imageUrl must be a valid HTTP/HTTPS URL")
    }

    UpdateBadgeImageRequest(imageUrl)
  }
}

