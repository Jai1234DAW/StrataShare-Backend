package dev.pompilius.users.domain.request

import dev.pompilius.country.domain.Country
import play.api.i18n.Lang

case class UpdateUserRequest(
    username: String,
    interests: Option[List[String]],
    phone: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    country: Country,
    language: Option[Lang],
    bio: Option[String]
)
