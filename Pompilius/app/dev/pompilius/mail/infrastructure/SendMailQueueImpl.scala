package dev.pompilius.mail.infrastructure

import dev.pompilius.mail.domain.MailSender
import org.apache.pekko.actor.{ActorSystem, Props}
import org.apache.pekko.pattern.ask
import org.apache.pekko.util.Timeout
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SendMailQueueImpl @Inject() (
    mailSender: MailSender,
    configuration: Configuration,
    system: ActorSystem
)(implicit val ec: ExecutionContext)
    extends SendMailQueue {

  private val logger = Logger(this.getClass)
  private val sendEmailQueueActor =
    system.actorOf(Props(classOf[SendEmailQueueActor], configuration), "SendEmailQueueActor")

  override def add(mail: Mail, accountId: Option[AccountId], key: Option[String]): Unit = {
    sendEmailQueueActor ! AddJob(
      Job(
        mail.to.address + "::" + key.getOrElse("") + "::" + mail.subject.map(_.subject).getOrElse("No Subject"),
        { () =>
          mailSender
            .sendMail(mail = mail, accountId = accountId)
            .recover {
              case NonFatal(e) =>
                logger.error(s"Error sending mail to: ${mail.to.address}", e)
            }
            .map(_ => ())
        }
      )
    )
  }

  override def getSize: Future[Int] = {
    sendEmailQueueActor.?(GetSize)(Timeout(10.seconds)).mapTo[Int]
  }

  override def getKeys(limit: Int): Future[Seq[String]] = {
    sendEmailQueueActor.?(GetKeys(limit))(Timeout(10.seconds)).mapTo[Seq[String]]
  }

  override def flush(): Unit = {
    sendEmailQueueActor ! Flush
  }
}

class SendEmailQueueActor(configuration: Configuration) extends BaseQueueActor {

  protected val initialDelay: FiniteDuration = configuration.mails.sendEmailQueueInitialDelay
  protected val interval: FiniteDuration = configuration.mails.sendEmailQueueInterval

}
