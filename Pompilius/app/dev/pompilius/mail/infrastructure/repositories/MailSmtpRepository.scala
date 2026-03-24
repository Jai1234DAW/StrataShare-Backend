package dev.pompilius.mail.infrastructure.repositories

import dev.pompilius.shared.domain.Configuration
import org.apache.pekko.Done
import dev.pompilius.mail.domain.{Mail, MailAddress, MailContent, MailRepository, MailSubject}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import org.apache.commons.mail.{Email, EmailConstants, HtmlEmail, MultiPartEmail, SimpleEmail}

// Implementación de MailRepository que envía correos utilizando SMTP
// Utiliza la biblioteca Apache Commons Email para construir y enviar los correos
// Configura el servidor SMTP, autenticación y otras opciones a través de la configuración proporcionada

class MailSmtpRepository @Inject() (
    configuration: Configuration
)(implicit val ec: ExecutionContext)
    extends MailRepository {

  override def sendSimpleMail(to: String, subject: String, text: String): Future[Done] = {
    val mail = Mail(
      to = MailAddress(to, None),
      subject = Some(MailSubject(subject)),
      content = MailContent(
        text = Some(text),
        html = None
      ),
      attachments = List.empty
    )
    sendMail(mail)
  }

  override def sendMail(mail: Mail): Future[Done] =
    Future {
      val email: Email = newEmail(mail)
      email.setCharset(EmailConstants.UTF_8);
      email.setHostName(configuration.mail.host)
      (configuration.mail.username, configuration.mail.password) match {
        case (Some(user), Some(pass)) =>
          email.setAuthentication(user, pass)
        case _ =>
      }
      configuration.mail.smtpPort.foreach(email.setSmtpPort)
      configuration.mail.sslSmtpPort.foreach(p => email.setSslSmtpPort(p.toString))
      configuration.mail.sslCheckServerIdentity.foreach(email.setSSLCheckServerIdentity)
      configuration.mail.startTlsEnabled.foreach(email.setStartTLSEnabled)
      configuration.mail.startTlsRequired.foreach(email.setStartTLSRequired)

      mail.subject.foreach(s => email.setSubject(s.subject))

      mail.to.name match {
        case Some(name) =>
          email.addTo(mail.to.address, name)
        case _ =>
          email.addTo(mail.to.address)
      }

      configuration.mail.from.name match {
        case Some(name) =>
          email.setFrom(configuration.mail.from.address, name)
        case _ =>
          email.setFrom(configuration.mail.from.address)
      }

      configuration.mail.replyTo.foreach { replyTo =>
        replyTo.name match {
          case Some(name) =>
            email.addReplyTo(replyTo.address, name)
          case _ =>
            email.addReplyTo(replyTo.address)
        }
      }

      email.send()
      Done
    }

  // Crea un objeto Email adecuado según el contenido y los adjuntos del mail
  private def newEmail(mail: Mail): Email = {
    if (mail.attachments.nonEmpty) {
      val m = new MultiPartEmail
      mail.content.text.foreach(m.setMsg)
      // Todo: agregar adjuntos al email
      m
    } else {
      mail.content.html match {
        case Some(_) =>
          val m = new HtmlEmail()
          mail.content.text.foreach(m.setTextMsg)
          mail.content.html.foreach(m.setHtmlMsg)
          m
        case None =>
          val m = new SimpleEmail()
          mail.content.text.foreach(m.setMsg)
          m
      }
    }
  }
}
