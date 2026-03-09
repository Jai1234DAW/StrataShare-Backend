package dev.pompilius.user.domain.request

import dev.pompilius.user.domain.Role

case class SetUserRolesRequest (roles: Set[Role])
