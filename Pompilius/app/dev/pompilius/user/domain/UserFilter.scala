package dev.pompilius.user.domain

import dev.pompilius.country.domain.Country

case class UserFilter(
    username: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    enabled: Option[Boolean] = None,
    country: Option[Country] = None,
    roleId: Option[RoleId] = None,
    search: Option[String] = None
)
