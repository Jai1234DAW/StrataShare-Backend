package dev.pompilius.user.domain

import dev.pompilius.country.domain.Country
import org.joda.time.DateTime
import play.api.i18n.Lang

case class User(
    id: UserId,
    username: String,
    passwordHash: String,
    country: Country,
    enabled: Boolean,
    email: String,
    firstName: Option[String],
    lastName: Option[String],
    phone: Option[String],
    created: DateTime,
    updated: DateTime,
    //Avatar:,
    language: Option[Lang],
    notes: Option[String],
    Bio: Option[String],
    //Mirar esto de aquí porque no estoy segura.
    roleId: RoleId
) {

  def fullName: String = {
    (firstName, lastName) match {
      case (Some(fn), Some(ln)) => s"$fn $ln"
      case (Some(fn), None)     => fn
      case (None, Some(ln))     => ln
      case (None, None)         => username
    }
  }

}

//Aquí puede haber más parámetros según se crean las otras cosas, los estudios por ejemplo
