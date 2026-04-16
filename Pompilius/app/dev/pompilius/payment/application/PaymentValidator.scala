package dev.pompilius.payment.application

import com.google.inject.ImplementedBy
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain._
import dev.pompilius.payment.domain.exceptions.PaymentCreateException
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Configuration
import dev.pompilius.transaction.application.TransactionService
import dev.pompilius.users.domain.User

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// Servicio de pagos que maneja la lógica de negocio para: Validar compras

@ImplementedBy(classOf[PaymentValidatorImpl])
trait PaymentValidator {
  def validatePurchase(resourceId: ResourceId, buyer: User): Future[PurchaseData]
}

@Singleton
class PaymentValidatorImpl @Inject() (
    transactionService: TransactionService,
    configuration: Configuration
)(implicit ec: ExecutionContext)
    extends PaymentValidator {

  override def validatePurchase(resourceId: ResourceId, buyer: User): Future[PurchaseData] = {
    for {

      feePompilius <- Future.successful(configuration.payments.feeOwnPlatform)

      // Usar TransactionService para validar la compra
      (resource, seller) <- transactionService.isAbleToPurchase(resourceId, buyer)

      // Calcular precios
      price <- resource.price match {
        case Some(p) => Future.successful(p)
        case None    => Future.failed(new PaymentCreateException("Resource price is required for purchase"))
      }

      //Calculo de precio + comisión de la plataforma
      fee = price * feePompilius

      //Calculo total a pagar del precio del recurso + plataforma de Stripe + comisión de la plataforma Pompilius
      totalAmount = price + fee

    } yield PurchaseData(
      resource = resource,
      seller = seller,
      buyer = buyer,
      price = price,
      fee = fee,
      totalAmount = totalAmount,
      currency = configuration.payments.currency,
      gateway = Gateway.STRIPE
    )
  }
}
