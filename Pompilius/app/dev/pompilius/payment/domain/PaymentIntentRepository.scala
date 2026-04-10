package dev.pompilius.payment.domain

import com.google.inject.ImplementedBy
import dev.pompilius.payment.infrastructure.repositories.PaymentIntentMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[PaymentIntentMySqlRepository])
trait PaymentIntentRepository {

  def save(paymentIntent: PaymentIntent): Future[Done]

  def create(intent: PaymentIntent):Future[Done]

  def findByPaymentId(paymentId: PaymentId): Future[Option[PaymentIntent]]

  def findByGatewayIntentId(gateway: Gateway, gatewayIntentId: String): Future[Option[PaymentIntent]]

  def updateStatus(paymentId: PaymentId, status: PaymentIntentStatus): Future[Done]

}
