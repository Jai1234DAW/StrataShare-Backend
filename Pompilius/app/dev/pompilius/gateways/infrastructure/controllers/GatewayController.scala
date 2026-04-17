package dev.pompilius.gateways.infrastructure.controllers

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.{Charge, PaymentIntent}
import com.stripe.net.Webhook
import dev.pompilius.Strings
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain._
import dev.pompilius.payment.domain.exceptions.PaymentNotFoundException
import dev.pompilius.resource.domain.{ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
import dev.pompilius.transaction.domain.exceptions.{TransactionNotAllowedException, TransactionNotFoundException}
import dev.pompilius.transaction.domain.{TransactionRepository, TransactionStatus}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, RawBuffer}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GatewayController @Inject() (
    paymentIntentRepository: PaymentIntentRepository,
    paymentRepository: PaymentRepository,
    transactionRepository: TransactionRepository,
    resourceUserRepository: ResourceUserRepository,
    clock: Clock,
    configuration: Configuration,
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

  // Configuración de Stripe
  private val stripeConfig = configuration.stripe
  private val webhookSecret = stripeConfig.webhookSecret
  private  val stripeApiKey = stripeConfig.secretKey

  /**
    * Endpoint que recibe notificaciones de Stripe cuando ocurren eventos importantes.
    *
    * ¿Por qué es necesario?
    * - Stripe envía notificaciones aquí cuando un pago se completa, falla o se cancela
    * - Es la ÚNICA forma confiable de confirmar pagos (el usuario puede cerrar el navegador)
    * - Garantiza que los recursos se asignen correctamente incluso si el frontend falla
    *
    * Seguridad:
    * - Valida la firma HMAC SHA256 de Stripe (webhook secret)
    * - Rechaza webhooks con firma inválida
    * - Previene ataques de terceros intentando simular eventos de Stripe
    */

  def handleStripeWebhook: Action[RawBuffer] =
    Action.async(parse.raw) { implicit request =>
      val payload = request.body.asBytes().map(_.utf8String).getOrElse("")
      val signature = request.headers.get("Stripe-Signature").getOrElse("")

      logger.error(s"WEBHOOK HIT method=${request.method} path=${request.path} contentType=${request.contentType}")
      // Validar firma del webhook usando la librería oficial de Stripe
      // Esto garantiza que el webhook realmente viene de Stripe y no fue alterado

      val validationResult =
        try {
          val event = Webhook.constructEvent(payload, signature, webhookSecret)
          logger.info(s"Webhook signature validated successfully for event: ${event.getType}")
          (true, Json.parse(payload))
        } catch {
          case e: SignatureVerificationException =>
            logger.error(s"SECURITY ALERT: Invalid webhook signature - Possible attack attempt: ${e.getMessage}")
            (false, Json.obj())
          case e: Exception =>
            logger.error(s"Error validating webhook: ${e.getMessage}", e)
            (false, Json.obj())
        }

      val (isValid, json) = validationResult

      if (!isValid) {
        logger.error("Rejecting webhook with invalid signature")
        Future.successful(Unauthorized(Json.obj("error" -> "Invalid signature")))
      } else {
        val eventType = (json \ "type").as[String]

        logger.info(s"Processing Stripe webhook event: $eventType")

        eventType match {
          // Eventos de Checkout Session
          case "checkout.session.completed" =>
            handleCheckoutSessionCompleted(json).map(_ => Ok(Json.obj("received" -> true)))

          case "checkout.session.expired" =>
            handleCheckoutSessionExpired(json).map(_ => Ok(Json.obj("received" -> true)))

          // Eventos de PaymentIntent (opcional, como fallback)
          case "payment_intent.succeeded" =>
            handlePaymentIntentSucceeded(json).map(_ => Ok(Json.obj("received" -> true)))

          case "payment_intent.payment_failed" =>
            handlePaymentIntentFailed(json).map(_ => Ok(Json.obj("received" -> true)))

          case "payment_intent.canceled" =>
            handlePaymentIntentCanceled(json).map(_ => Ok(Json.obj("received" -> true)))

          case _ =>
            logger.info(s"Unhandled event type: $eventType")
            Future.successful(Ok(Json.obj("received" -> true)))
        }
      }
    }

  def oneTimePaymentCompleted(gatewayName: String, paymentIntentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val gateway = Gateway.withNameInsensitive(gatewayName) match {
            case Gateway.STRIPE => Gateway.STRIPE
            case _              => throw new PaymentNotFoundException(s"Unsupported gateway: $gatewayName")
          }

          val pid = PaymentId(paymentIntentId)

          for {
            paymentIntent <-
              paymentIntentRepository
                .findByPaymentId(pid)
                .map(_.getOrElse(throw new PaymentNotFoundException(s"PaymentIntent $paymentIntentId not found")))

            transaction <-
              transactionRepository
                .findById(paymentIntent.transactionId)
                .map(_.getOrElse(throw new TransactionNotFoundException("Transaction not found")))

            _ = if (transaction.buyerId != user.id) {
              throw new TransactionNotAllowedException("You are not authorized")
            }

            _ = if (paymentIntent.gateway != gateway) {
              throw new TransactionNotAllowedException("This payment does not belong to the provided gateway")
            }

          } yield {
            val parameters =
              paymentIntent.returnUrlParams.getOrElse(Map.empty[String, String]) ++ Map(
                Strings.resourceId -> paymentIntent.resourceId.toString,
                Strings.sellerId -> transaction.sellerId.toString,
                Strings.paymentId -> paymentIntent.paymentId.toString
              )

            val url = configuration.payments.paymentCompletedUrl

            TemporaryRedirect(
              UrlUtil.addQueryParameters(
                UrlUtil.interpolateVariables(url, parameters),
                parameters
              )
            )
          }
      }
    }

  @SuppressWarnings(Array("UnusedMethodParameter"))
  def oneTimePaymentCanceled(gatewayName: String, paymentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val gateway = Gateway.withNameInsensitive(gatewayName) match {
            case Gateway.STRIPE => Gateway.STRIPE
            case _              => throw new PaymentNotFoundException(s"Unsupported gateway: $gatewayName")
          }

          val pid = PaymentId(paymentId)

          for {
            paymentIntent <-
              paymentIntentRepository
                .findByPaymentId(pid)
                .map(_.getOrElse(throw new PaymentNotFoundException(s"PaymentIntent $paymentId not found")))

            transaction <-
              transactionRepository
                .findById(paymentIntent.transactionId)
                .map(_.getOrElse(throw new TransactionNotFoundException("Transaction not found")))

            _ = if (transaction.buyerId != user.id) {
              throw new TransactionNotAllowedException("You are not authorized")
            }

            _ = if (paymentIntent.gateway != gateway) {
              throw new TransactionNotAllowedException("This payment does not belong to the provided gateway")
            }

          } yield {
            val parameters =
              paymentIntent.returnUrlParams.getOrElse(Map.empty[String, String]) ++ Map(
                Strings.resourceId -> paymentIntent.resourceId.toString,
                Strings.sellerId -> transaction.sellerId.toString,
                Strings.paymentId -> paymentIntent.paymentId.toString
              )

            val url = configuration.payments.paymentCanceledUrl

            TemporaryRedirect(
              UrlUtil.addQueryParameters(
                UrlUtil.interpolateVariables(url, parameters),
                parameters
              )
            )
          }
      }
    }
  // Maneja el evento cuando una sesión de checkout se completa exitosamente. Este es el evento principal para Stripe Checkout

  private def handleCheckoutSessionCompleted(json: JsValue): Future[Unit] = {
    val sessionObj = json \ "data" \ "object"
    val sessionId = (sessionObj \ "id").as[String]
    val metadata = (sessionObj \ "metadata").asOpt[Map[String, String]].getOrElse(Map.empty)
    val paymentIntentId = (sessionObj \ "payment_intent").asOpt[String]
    val paymentIdStr = metadata.getOrElse("paymentId", "")
    val amountTotal = (sessionObj \ "amount_total").asOpt[Long].getOrElse(0L)
    val currency = (sessionObj \ "currency").asOpt[String].getOrElse("eur")
    val paymentStatus = (sessionObj \ "payment_status").asOpt[String].getOrElse("")

    logger.info(s"Checkout session completed: $sessionId, paymentId: $paymentIdStr, status: $paymentStatus")

    if (paymentStatus != "paid") {
      logger.warn(s"Checkout session $sessionId completed but payment status is: $paymentStatus")
      return Future.successful(())
    }

    for {
      // Buscar el PaymentIntent en nuestra BD usando el sessionId (que guardamos como gatewayIntentId)
      paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
      paymentIntent = paymentIntentOpt.getOrElse(
        throw new PaymentNotFoundException(s"PaymentIntent not found for session: $sessionId")
      )

      // Actualizar el PaymentIntent a SUCCEEDED
      _ <- paymentIntentRepository.updateStatus(paymentIntent.paymentId, PaymentIntentStatus.SUCCEEDED)

      // Buscar la Transaction
      transaction <-
        transactionRepository
          .findById(paymentIntent.transactionId)
          .map(
            _.getOrElse(throw new TransactionNotFoundException(s"Transaction ${paymentIntent.transactionId} not found"))
          )

      // Actualizar Transaction a COMPLETED
      _ <- transactionRepository.updateStatus(transaction.id, TransactionStatus.COMPLETED)

      // CREAR Payment (registro final del pago exitoso)
      paymentId = paymentIntent.paymentId

      // Calcular montos
      amountBigDecimal = BigDecimal(amountTotal) / 100
      stripeFee = amountBigDecimal * BigDecimal(0.029) + BigDecimal(0.30)
      netAmount = amountBigDecimal - stripeFee

      // Obtener la URL del recibo si está disponible (necesitamos el PaymentIntent)
      receiptUrl <- paymentIntentId match {
        case Some(piId) =>
          Future {
            try {
              val pi = PaymentIntent.retrieve(piId)
              Option(pi.getLatestCharge).flatMap { chargeId =>
                try {
                  val charge = Charge.retrieve(chargeId)
                  Option(charge.getReceiptUrl)
                } catch {
                  case _: Exception => None
                }
              }
            } catch {
              case e: Exception =>
                logger.warn(s"Could not retrieve receipt URL: ${e.getMessage}")
                None
            }
          }
        case None => Future.successful(None)
      }

      payment = Payment(
        id = paymentId,
        transactionId = transaction.id,
        gateway = Gateway.STRIPE,
        gatewayPaymentId = paymentIntentId.getOrElse(sessionId), // Usamos el PaymentIntent ID si está disponible
        amount = amountBigDecimal,
        netAmount = netAmount,
        currency = currency,
        receiptUrl = receiptUrl,
        instrument = paymentIntent.instrument,
        refunded= false,
        refundedAmount = BigDecimal(0),
        created = clock.now,
        updated = clock.now,
        metadata = Some(Json.obj("sessionId" -> sessionId, "paymentIntentId" -> paymentIntentId).toString)
      )

      // Guardar Payment
      _ <- paymentRepository.create(payment)

      // Dar acceso al recurso al comprador
      _ <- resourceUserRepository.save(
        ResourceUser(
          resourceId = transaction.resourceId,
          userId = transaction.buyerId,
          resourceUserType = ResourceUserType.PURCHASED,
          created = clock.now
        )
      )

      _ = logger.info(
        s"Purchase completed successfully for transaction: ${transaction.id}, payment: ${paymentId.toString}"
      )

    } yield ()
  }

  // Manejo del evento cuando una sesión de checkout expira
  private def handleCheckoutSessionExpired(json: JsValue): Future[Unit] = {
    val sessionObj = json \ "data" \ "object"
    val sessionId = (sessionObj \ "id").as[String]

    logger.info(s"Checkout session expired: $sessionId")

    for {
      paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
      _ <- paymentIntentOpt match {
        case Some(pi) =>
          for {
            _ <- paymentIntentRepository.updateStatus(pi.paymentId, PaymentIntentStatus.CANCELED)
            _ <- transactionRepository.updateStatus(pi.transactionId, TransactionStatus.CANCELED)
          } yield ()
        case None =>
          logger.warn(s"PaymentIntent not found for expired session: $sessionId")
          Future.successful(())
      }
    } yield ()
  }

  //Handler para payment_intent.succeeded (opcional, como backup) En Checkout Sessions, normalmente se usa checkout.session.completed
  private def handlePaymentIntentSucceeded(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val paymentIntentId = (paymentIntentObj \ "id").as[String]

    logger.info(s"PaymentIntent succeeded: $paymentIntentId (this might be handled by checkout.session.completed)")

    // En checkout sessions, esto es redundante con checkout.session.completed
    // Podemos simplemente loguear o ignorar
    Future.successful(())
  }

  // Handler para payment_intent.payment_failed
  private def handlePaymentIntentFailed(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val sessionId = (paymentIntentObj \ "id").as[String]

    logger.warn(s"PaymentIntent failed: $sessionId")

    for {
      paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
      _ <- paymentIntentOpt match {
        case Some(pi) =>
          for {
            _ <- paymentIntentRepository.updateStatus(pi.paymentId, PaymentIntentStatus.FAILED)
            _ <- transactionRepository.updateStatus(pi.transactionId, TransactionStatus.FAILED)
          } yield ()
        case None =>
          logger.warn(s"PaymentIntent not found  for session: $sessionId")
          Future.successful(())
      }
    } yield ()
  }

  //Handler para payment_intent.canceled

  private def handlePaymentIntentCanceled(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val sessionId = (paymentIntentObj \ "id").as[String]

    logger.info(s"PaymentIntent canceled: $sessionId")

    for {
      paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
      _ <- paymentIntentOpt match {
        case Some(pi) =>
          for {
            _ <- paymentIntentRepository.updateStatus(pi.paymentId, PaymentIntentStatus.CANCELED)
            _ <- transactionRepository.updateStatus(pi.transactionId, TransactionStatus.CANCELED)
          } yield ()
        case None =>
          logger.warn(s"PaymentIntent not found for session: $sessionId")
          Future.successful(())
      }
    } yield ()
  }
}
