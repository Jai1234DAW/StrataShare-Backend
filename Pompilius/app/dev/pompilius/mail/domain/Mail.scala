package dev.pompilius.mail.domain

case class Mail(
    to: MailAddress,
    subject: Option[MailSubject],
    content: MailContent,
    attachments: List[MailAttachment] = List.empty[MailAttachment]
)
