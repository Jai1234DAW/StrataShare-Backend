package dev.pompilius.gateways.infrastructure

import dev.pompilius.gateways.domain.{Gateway, PaymentGateway}
import dev.pompilius.shared.domain.exceptions.BadRequestException

import javax.inject.{Inject, Singleton}

// Factory para obtener la implementación de PaymentGateway adecuada según el Gateway solicitado
@Singleton
class PaymentGatewayFactory @Inject() (
    stripePaymentGateway: StripePaymentGateway
    // Aquí se inyectarían otras implementaciones en el futuro:
) {

  def getGateway(gateway: Gateway): PaymentGateway = {
    gateway match {
      case Gateway.STRIPE | Gateway.STRIPE_MOBILE =>
        stripePaymentGateway

      // Implementaciones futuras:
      // case Gateway.PAYPAL =>
      //   paypalPaymentGateway
      //

      case _ =>
        throw new BadRequestException(s"Payment gateway ${gateway.value} is not supported")
    }
  }
}

