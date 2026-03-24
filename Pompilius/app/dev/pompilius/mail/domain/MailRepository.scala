package dev.pompilius.mail.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done
import dev.pompilius.mail.infrastructure.repositories.MailSmtpRepository
import scala.concurrent.Future

@ImplementedBy(classOf[MailSmtpRepository])
trait MailRepository {

  def sendSimpleMail(to: String, subject: String, text: String): Future[Done]

  def sendMail(mail: Mail): Future[Done]

}
