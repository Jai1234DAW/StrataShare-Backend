package dev.pompilius.transaction.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.transaction.infrastructure.repositories.TransactionMySqlRepository
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[TransactionMySqlRepository])
trait TransactionRepository {

  def save(transaction: Transaction): Future[Done]

  def findById(id: TransactionId): Future[Option[Transaction]]

  def find(filter: TransactionFilter, pag: Pagination): Future[List[Transaction]]

  def updateStatus(id: TransactionId, status: TransactionStatus): Future[Done]

  def delete(id: TransactionId): Future[Done]
}

