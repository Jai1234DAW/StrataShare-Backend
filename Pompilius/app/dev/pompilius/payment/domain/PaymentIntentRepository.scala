package dev.pompilius.payment.domain

import com.google.inject.ImplementedBy
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.infrastructure.repositories.PaymentIntentMySqlRepository
import dev.pompilius.transaction.domain.TransactionId
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[PaymentIntentMySqlRepository])
trait PaymentIntentRepository {

  def save(paymentIntent: PaymentIntent): Future[Done]

  def findByPaymentId(paymentId: PaymentId): Future[Option[PaymentIntent]]

  def findByPaymentIdAndTransactionId(paymentId: PaymentId, transactionId: TransactionId): Future[Option[PaymentIntent]]

  def findByGatewayIntentId(gateway: Gateway, gatewayIntentId: String): Future[Option[PaymentIntent]]

  def updateStatus(paymentId: PaymentId, status: PaymentIntentStatus): Future[Done]

  def updateStatusByCompositeKey(paymentId: PaymentId, transactionId: TransactionId, status: PaymentIntentStatus): Future[Done]

  def markSucceededIfNotSucceeded(paymentId: PaymentId): Future[Int]

  def markSucceededIfNotSucceededByCompositeKey(paymentId: PaymentId, transactionId: TransactionId): Future[Int]
}
