package dev.pompilius.payment.domain

import com.google.inject.ImplementedBy
import dev.pompilius.payment.infrastructure.repositories.PaymentMySqlRepository
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.transaction.domain.TransactionId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[PaymentMySqlRepository])
trait PaymentRepository {

  def create(payment: Payment): Future[Done]

  def findById(id: PaymentId): Future[Option[Payment]]

  def findByIdAndTransactionId(id: PaymentId, transactionId: TransactionId): Future[Option[Payment]]

  def findByGatewayPaymentId(gateway: String, gatewayPaymentId: String): Future[Option[Payment]]

  def find(filter: PaymentFilter, pag: Pagination): Future[List[Payment]]

  def findByTransactionId(transactionId: TransactionId): Future[Option[Payment]]

  //Aqui luego se pueden implementar métodos que sirvan al administrador de la empresa para gestionnar incidencias

}
