package dev.pompilius.mail.domain

case class MailContent(text: Option[String], html: Option[String])

object MailContent {

  def apply(text: String): MailContent = {
    MailContent(
      text = Some(text),
      html = None
    )
  }

}
