package dev.pompilius.mail.domain

case class MailAddress(
    address: String,
    name: Option[String] = None
)