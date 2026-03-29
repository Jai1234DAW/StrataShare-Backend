package dev.pompilius.users.domain.request

import dev.pompilius.country.domain.Country
import play.api.i18n.Lang
import dev.pompilius.users.domain.Role

case class RegisterUserRequest(
    username: String,
    password: String,
    email: String,
    phone: Option[String],
    firstName: Option[String],
    lastName: Option[String],
    country: Country,
    language: Option[Lang],
    notes: Option[String],
    bio: Option[String],
    role: Role
)
