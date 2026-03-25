package dev.pompilius.users.domain.request

import dev.pompilius.country.domain.Country

case class SearchUsersRequest(
    username: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    country: Option[Country] = None,
    search: Option[String] = None,
    limit: Option[Int] = None,
    offset: Int = 0
)


