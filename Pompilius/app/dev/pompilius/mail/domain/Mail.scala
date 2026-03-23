package dev.pompilius.mail.domain

case class Mail(
    to: MailAddress,
    subject: Option[MailSubject],
    content: List[MailContent],
    attachments: List[MailAttachment]
)

object Mail {
  def apply(
      to: MailAddress,
      subject: Option[MailSubject],
      content: MailContent
  ): Mail = {
    Mail(
      to = to,
      subject = subject,
      content = List(content),
      attachments = List.empty[MailAttachment]
    )
  }

  def apply(
      to: MailAddress,
      subject: Option[MailSubject],
      content: MailContent,
      attachments: List[MailAttachment]
  ): Mail = {
    Mail(
      to = to,
      subject = subject,
      content = List(content),
      attachments = attachments
    )
  }
}
