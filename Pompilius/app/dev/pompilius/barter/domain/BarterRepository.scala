package dev.pompilius.barter.domain

import com.google.inject.ImplementedBy
import dev.pompilius.barter.infrastructure.repositories.BarterMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[BarterMySqlRepository])
trait BarterRepository {

  def save(barter: Barter): Future[Done]

  def findByTransactionId(transactionId: TransactionId): Future[Option[Barter]]

  def find(filter: BarterFilter, pag: Pagination): Future[List[Barter]]
}
