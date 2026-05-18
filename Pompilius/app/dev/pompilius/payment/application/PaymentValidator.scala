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

      platformFeePercentage <- Future.successful(configuration.payments.feeOwnPlatform)

      // Usar TransactionService para validar la compra
      (resource, seller) <- transactionService.isAbleToPurchase(resourceId, buyer)

      // Calcular precios
      price <- resource.price match {
        case Some(p) => Future.successful(p)
        case None    => Future.failed(new PaymentCreateException("Resource price is required for purchase"))
      }

      // Comisión de la plataforma (la pagará el vendedor, descontada de lo que recibe)
      platformFee = price * platformFeePercentage

      // Total a pagar = solo el precio (el surcharge de Stripe se añade después en PaymentCreator)
      totalAmount = price

    } yield PurchaseData(
      resource = resource,
      seller = seller,
      buyer = buyer,
      price = price,
      fee = platformFee,
      totalAmount = totalAmount,
      currency = configuration.payments.currency,
      gateway = Gateway.STRIPE
    )
  }
}
