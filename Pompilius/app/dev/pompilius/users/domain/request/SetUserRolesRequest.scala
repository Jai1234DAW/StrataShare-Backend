package dev.pompilius.users.domain.request

import dev.pompilius.users.domain.Role

case class SetUserRolesRequest (roles: Set[Role])
