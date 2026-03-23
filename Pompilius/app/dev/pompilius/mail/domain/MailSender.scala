package dev.pompilius.mail.domain

import org.apache.pekko.Done
import com.google.inject.ImplementedBy


import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MailSenderImpl])
trait MailSender {
  def sendMail(mail: Mail, accountId: Option[AccountId]): Future[Done]
}

@Singleton
class MailSenderImpl @Inject() (
    mailRepository: MailRepository
)(implicit ec: ExecutionContext)
    extends MailSender {

  override def sendMail(mail: Mail, accountId: Option[AccountId]): Future[Done] = {
    for {
      _ <- mailRepository.sendMail(mail, accountId: Option[AccountId])
    } yield {
      Done
    }
  }

}
