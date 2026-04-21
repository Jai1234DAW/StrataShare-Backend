package dev.pompilius.users.domain

import dev.pompilius.country.domain.Country
import dev.pompilius.attachment.domain.{Attachment, AttachmentId}
import dev.pompilius.users.domain.UserId
import org.joda.time.DateTime
import play.api.i18n.Lang

case class User(
    id: UserId,
    username: String,
    passwordHash: String,
    enabled: Boolean,
    email: String,
    interests: Option[List[String]],
    country: Country,
    firstName: Option[String],
    lastName: Option[String],
    phone: Option[String],
    language: Option[Lang],
    created: DateTime,
    updated: DateTime,
    avatar: Option[AttachmentId],
    notes: Option[String],
    bio: Option[String]
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
