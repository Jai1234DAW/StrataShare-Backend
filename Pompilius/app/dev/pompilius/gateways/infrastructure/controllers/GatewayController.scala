package dev.pompilius.gateways.infrastructure.controllers

import com.stripe.exception.SignatureVerificationException
import com.stripe.model.checkout.Session
import com.stripe.model.{Charge, PaymentIntent => StripePaymentIntent}
import com.stripe.net.Webhook
import dev.pompilius.Strings
import dev.pompilius.badge.application.BadgeService
import dev.pompilius.event.domain.EventU
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain._
import dev.pompilius.payment.domain.exceptions.PaymentNotFoundException
import dev.pompilius.resource.domain.{ResourceUser, ResourceUserRepository, ResourceUserType}
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
import dev.pompilius.transaction.domain.exceptions.{TransactionNotAllowedException, TransactionNotFoundException}
import dev.pompilius.transaction.domain.{Transaction, TransactionRepository, TransactionStatus}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, RawBuffer}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class GatewayController @Inject() (
    paymentIntentRepository: PaymentIntentRepository,
    paymentRepository: PaymentRepository,
    transactionRepository: TransactionRepository,
    resourceUserRepository: ResourceUserRepository,
    badgeService: BadgeService,
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

            _ <-
              paymentIntent.status match {
                case PaymentIntentStatus.SUCCEEDED =>
                  logger.info(s"PaymentIntent ${paymentIntent.paymentId} already SUCCEEDED, skipping fallback")
                  Future.successful(())

                case _ =>
                  val sessionId = paymentIntent.gatewayIntentId

                  if (sessionId == null || sessionId.trim.isEmpty) {
                    Future.failed(
                      new PaymentNotFoundException(
                        s"No gatewayIntentId/sessionId found for paymentIntent ${paymentIntent.paymentId}"
                      )
                    )
                  } else {
                    Future {
                      logger.info(s"Fallback: retrieving Stripe Checkout Session $sessionId")

                      val session = Session.retrieve(sessionId)

                      val sessionStatus = Option(session.getStatus).getOrElse("")
                      val paymentStatus = Option(session.getPaymentStatus).getOrElse("")

                      val isPaid =
                        paymentStatus == "paid" || sessionStatus == "complete"

                      if (!isPaid) {
                        throw new TransactionNotAllowedException(
                          s"Payment not completed. session=$sessionId status=$sessionStatus payment_status=$paymentStatus"
                        )
                      }

                      val amountTotal = Option(session.getAmountTotal).map(_.longValue()).getOrElse(0L)
                      val currency = Option(session.getCurrency).getOrElse("eur")
                      val stripePaymentIntentId = Option(session.getPaymentIntent).map(_.toString)

                      (sessionId, amountTotal, currency, stripePaymentIntentId)
                    }.flatMap {
                      case (sessionId, amountTotal, currency, stripePaymentIntentId) =>
                        for {
                          receiptUrl <- fetchReceiptUrl(stripePaymentIntentId)
                          _ <- reconcileSuccessfulCheckout(
                            paymentIntent = paymentIntent,
                            transaction = transaction,
                            sessionId = sessionId,
                            stripePaymentIntentId = stripePaymentIntentId,
                            amountTotalInCents = amountTotal,
                            currency = currency,
                            receiptUrl = receiptUrl
                          )
                        } yield ()
                    }
                  }
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
      logger.info(s"Payment cancel endpoint hit for gatewayName=$gatewayName paymentId=$paymentId")
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

            _ <- reconcileCanceledCheckout(
              paymentIntent = paymentIntent,
              transaction = transaction
            )

          } yield {
            val parameters =
              paymentIntent.returnUrlParams.getOrElse(Map.empty[String, String]) ++ Map(
                Strings.resourceId -> paymentIntent.resourceId.toString,
                Strings.sellerId -> transaction.sellerId.toString,
                Strings.paymentId -> paymentIntent.paymentId.toString
              )

            val url = configuration.payments.paymentCanceledUrl

            logger.info(s"Redirecting to cancel URL: $url with parameters: $parameters")
            TemporaryRedirect(
              UrlUtil.addQueryParameters(
                UrlUtil.interpolateVariables(url, parameters),
                parameters
              )
            )
          }
      }
    }

  private def handleCheckoutSessionCompleted(json: JsValue): Future[Unit] = {
    val sessionObj = json \ "data" \ "object"
    val sessionId = (sessionObj \ "id").as[String]
    val paymentStatus = (sessionObj \ "payment_status").asOpt[String].getOrElse("")
    val amountTotal = (sessionObj \ "amount_total").asOpt[Long].getOrElse(0L)
    val currency = (sessionObj \ "currency").asOpt[String].getOrElse("eur")
    val stripePaymentIntentId = (sessionObj \ "payment_intent").asOpt[String]

    logger.info(
      s"Checkout session completed received: sessionId=$sessionId payment_status=$paymentStatus amount_total=$amountTotal"
    )

    if (paymentStatus != "paid") {
      logger.warn(s"Checkout session $sessionId completed but payment_status=$paymentStatus")
      Future.successful(())
    } else {
      for {
        paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
        paymentIntent = paymentIntentOpt.getOrElse(
          throw new PaymentNotFoundException(s"PaymentIntent not found for session: $sessionId")
        )

        transaction <-
          transactionRepository
            .findById(paymentIntent.transactionId)
            .map(_.getOrElse(throw new TransactionNotFoundException(s"Transaction ${paymentIntent.transactionId} not found")))

        receiptUrl <- fetchReceiptUrl(stripePaymentIntentId)

        _ <- reconcileSuccessfulCheckout(
          paymentIntent = paymentIntent,
          transaction = transaction,
          sessionId = sessionId,
          stripePaymentIntentId = stripePaymentIntentId,
          amountTotalInCents = amountTotal,
          currency = currency,
          receiptUrl = receiptUrl
        )
      } yield ()
    }
  }

  private def handleCheckoutSessionExpired(json: JsValue): Future[Unit] = {
    val sessionObj = json \ "data" \ "object"
    val sessionId = (sessionObj \ "id").as[String]

    logger.info(s"Checkout session expired: $sessionId")

    for {
      paymentIntentOpt <- paymentIntentRepository.findByGatewayIntentId(Gateway.STRIPE, sessionId)
      _ <- paymentIntentOpt match {
        case Some(pi) =>
          for {
            transaction <-
              transactionRepository
                .findById(pi.transactionId)
                .map(_.getOrElse(throw new TransactionNotFoundException(s"Transaction ${pi.transactionId} not found")))

            _ <- reconcileCanceledCheckout(
              paymentIntent = pi,
              transaction = transaction
            )
          } yield ()

        case None =>
          logger.warn(s"PaymentIntent not found for expired session: $sessionId")
          Future.successful(())
      }
    } yield ()
  }

  private def handlePaymentIntentSucceeded(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val stripePaymentIntentId = (paymentIntentObj \ "id").as[String]

    logger.info(
      s"Stripe payment_intent.succeeded received for pi=$stripePaymentIntentId. " +
        s"Handled mainly via checkout.session.completed in this integration."
    )

    Future.successful(())
  }

  private def handlePaymentIntentFailed(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val stripePaymentIntentId = (paymentIntentObj \ "id").as[String]

    logger.warn(
      s"Stripe payment_intent.payment_failed received for pi=$stripePaymentIntentId. " +
        s"No DB update is performed because this integration stores sessionId (cs_...) as gatewayIntentId."
    )

    Future.successful(())
  }

  private def reconcileCanceledCheckout(
      paymentIntent: dev.pompilius.payment.domain.PaymentIntent,
      transaction: Transaction
  ): Future[Unit] = {

    for {
      _ <- paymentIntentRepository.updateStatus(paymentIntent.paymentId, PaymentIntentStatus.CANCELED)
      _ <- transactionRepository.updateStatusRejectedCancelled(transaction.id, TransactionStatus.CANCELLED)
    } yield {
      logger.info(
        s"Payment canceled successfully for transaction=${transaction.id}, payment=${paymentIntent.paymentId}"
      )
    }
  }

  private def handlePaymentIntentCanceled(json: JsValue): Future[Unit] = {
    val paymentIntentObj = json \ "data" \ "object"
    val stripePaymentIntentId = (paymentIntentObj \ "id").as[String]

    logger.info(
      s"Stripe payment_intent.canceled received for pi=$stripePaymentIntentId. " +
        s"No DB update is performed because this integration stores sessionId (cs_...) as gatewayIntentId."
    )

    Future.successful(())
  }

  private def reconcileSuccessfulCheckout(
   paymentIntent:dev.pompilius.payment.domain.PaymentIntent,
   transaction: Transaction,
   sessionId: String,
   stripePaymentIntentId: Option[String],
   amountTotalInCents: Long,
   currency: String,
   receiptUrl: Option[String]
  ): Future[Unit] = {

    paymentIntentRepository.markSucceededIfNotSucceeded(paymentIntent.paymentId).flatMap { updatedRows =>
      if (updatedRows == 0) {
        logger.info(s"PaymentIntent ${paymentIntent.paymentId} already reconciled by another process")
        Future.successful(())
      } else {
        // Monto total que pagó el comprador (solo el precio del producto, sin surcharge)
        val amount = BigDecimal(amountTotalInCents) / 100

        // Obtener el fee real de Stripe desde el BalanceTransaction
        val gatewayFee = fetchStripeActualFee(stripePaymentIntentId).getOrElse(
          // Fallback: estimación si no se puede obtener (2.9% + 0.30€)
          amount * BigDecimal(0.029) + BigDecimal(0.30)
        )

        // Comisión de la plataforma (viene del Transaction.fee, calculada en PaymentValidator)
        val platformFee = transaction.fee.getOrElse(BigDecimal(0))

        // Monto neto que recibe el vendedor = amount - gatewayFee - platformFee
        // El vendedor absorbe tanto el fee de Stripe como la comisión de la plataforma
        val netAmount = amount - gatewayFee - platformFee

        logger.info(
          s"Payment breakdown for ${paymentIntent.paymentId}: " +
          s"amount=${amount}, gatewayFee=${gatewayFee}, platformFee=${platformFee}, netAmount=${netAmount}"
        )

        val payment = Payment(
          id = paymentIntent.paymentId,
          transactionId = transaction.id,
          gateway = Gateway.STRIPE,
          gatewayPaymentId = stripePaymentIntentId.getOrElse(sessionId),
          amount = amount,
          platformFee = platformFee,
          gatewayFee = gatewayFee,
          netAmount = netAmount,
          currency = currency,
          receiptUrl = receiptUrl,
          instrument = paymentIntent.instrument,
          refunded = false,
          refundedAmount = BigDecimal(0),
          created = clock.now,
          updated = clock.now,
          metadata = Some(
            Json.obj(
              "sessionId" -> sessionId,
              "paymentIntentId" -> stripePaymentIntentId
            ).toString
          )
        )

        for {
          _ <- transactionRepository.updateStatusCompleted(transaction.id, TransactionStatus.COMPLETED)
          _ <- paymentRepository.create(payment)
          _ <- resourceUserRepository.save(
            ResourceUser(
              resourceId = transaction.resourceId,
              userId = transaction.buyerId,
              resourceUserType = ResourceUserType.PURCHASED,
              created = clock.now,
              updated = clock.now
            )
          )

          // Registrar evento y verificar badges para el comprador. Esto es para lo de los logros
          earnedBadges <- badgeService.registerEventAndCheckBadges(transaction.buyerId, EventU.PURCHASE_COMPLETED)

          _ = if (earnedBadges.nonEmpty) {
            logger.info(
              s"User ${transaction.buyerId.id} earned ${earnedBadges.length} badge(s) after purchase: ${earnedBadges.map(_.name).mkString(", ")}"
            )
          }
          _ = logger.info(
            s"Purchase reconciled successfully for transaction=${transaction.id}, payment=${payment.id}, session=$sessionId"
          )
        } yield ()
      }
    }
  }

  private def fetchReceiptUrl(stripePaymentIntentId: Option[String]): Future[Option[String]] =
    stripePaymentIntentId match {
      case Some(piId) =>
        Future {
          try {
            val pi = StripePaymentIntent.retrieve(piId)
            Option(pi.getLatestCharge).flatMap { chargeId =>
              try {
                val charge = Charge.retrieve(chargeId)
                Option(charge.getReceiptUrl)
              } catch {
                case NonFatal(e) =>
                  logger.warn(s"Could not retrieve Charge $chargeId: ${e.getMessage}")
                  None
              }
            }
          } catch {
            case NonFatal(e) =>
              logger.warn(s"Could not retrieve Stripe PaymentIntent $piId: ${e.getMessage}")
              None
          }
        }

      case None =>
        Future.successful(None)
    }

  /**
    * Obtiene el fee REAL que cobró Stripe desde el BalanceTransaction.
    * Esto es más preciso que calcular 2.9% + 0.30€ porque Stripe puede tener
    * diferentes tarifas según el país, tipo de tarjeta, etc.
    */
  private def fetchStripeActualFee(stripePaymentIntentId: Option[String]): Option[BigDecimal] = {
    stripePaymentIntentId.flatMap { piId =>
      try {
        val pi = StripePaymentIntent.retrieve(piId)
        Option(pi.getLatestCharge).flatMap { chargeId =>
          try {
            val charge = Charge.retrieve(chargeId)
            Option(charge.getBalanceTransaction).map { balanceTransactionId =>
              try {
                val balanceTransaction = com.stripe.model.BalanceTransaction.retrieve(balanceTransactionId)
                // El fee viene en centavos
                BigDecimal(balanceTransaction.getFee) / 100
              } catch {
                case NonFatal(e) =>
                  logger.warn(s"Could not retrieve BalanceTransaction $balanceTransactionId: ${e.getMessage}")
                  BigDecimal(0)
              }
            }
          } catch {
            case NonFatal(e) =>
              logger.warn(s"Could not retrieve Charge $chargeId: ${e.getMessage}")
              None
          }
        }
      } catch {
        case NonFatal(e) =>
          logger.warn(s"Could not retrieve Stripe PaymentIntent $piId: ${e.getMessage}")
          None
      }
    }
  }
}