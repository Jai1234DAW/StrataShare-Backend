package dev.pompilius.mail.domain

case class MailContent(contentType: String, content: String)

object MailContent {

  def apply(content: String): MailContent = {
    MailContent(
      contentType = "text/plain",
      content = content
    )
  }

}