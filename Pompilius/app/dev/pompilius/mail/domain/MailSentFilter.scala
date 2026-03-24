package dev.pompilius.mail.domain

import dev.pompilius.users.domain.UserId

case class MailSentFilter(
    mailType: Option[MailType] = None,
    address: Option[String] = None,
    userId: Option[UserId] = None
)