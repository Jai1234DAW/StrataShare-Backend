package dev.pompilius.mail.domain

import org.joda.time.DateTime
import dev.pompilius.users.domain.UserId

case class MailSent(
    id: MailSentId,
    mailType: MailType,
    address: String,
    sentAt: DateTime,
    userId: UserId,
    metadata: Option[String]
)
