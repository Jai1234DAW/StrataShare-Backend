package dev.pompilius.mail.domain

import com.google.inject.ImplementedBy
import dev.pompilius.mail.infrastructure.repositories.mysql.MailSentMySqlRepository
import dev.pompilius.shared.domain.Pagination
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[MailSentMySqlRepository])
trait MailSentRepository {

  def findById(MailSentId: MailSentId): Future[Option[MailSent]]

  def find(filter: MailSentFilter, pag: Pagination): Future[List[MailSent]]

  def save(partner: MailSent): Future[Done]

  def delete(partnerId: MailSentId): Future[Done]

}
