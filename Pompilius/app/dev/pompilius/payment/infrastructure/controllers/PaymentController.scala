package dev.pompilius.payment.infrastructure.controllers

import dev.pompilius.payment.application.{PaymentCreator, PaymentValidator}
import dev.pompilius.payment.domain._
import dev.pompilius.payment.domain.exceptions.PaymentNotFoundException
import dev.pompilius.payment.infrastructure.parsers.CreatePaymentRequestParser
import dev.pompilius.payment.infrastructure.writers.{PaymentIntentWriter, PaymentWriter}
import dev.pompilius.shared.domain.{Clock, Paginated, Pagination}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.shared.infrastructure.writers.PaginatedWriter
import dev.pompilius.transaction.domain._
import dev.pompilius.transaction.domain.exceptions.{TransactionNotAllowedException, TransactionNotFoundException}
import dev.pompilius.users.domain.Role
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class PaymentController @Inject() (
    paymentCreator: PaymentCreator,
    paymentValidator: PaymentValidator,
    paymentIntentRepository: PaymentIntentRepository,
    paymentRepository: PaymentRepository,
    transactionRepository: TransactionRepository,
    paymentIntentWriter: PaymentIntentWriter,
    paginatedWriter: PaginatedWriter,
    paymentWriter: PaymentWriter,
    clock: Clock
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

  def createPaymentIntent: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL, Role.AMATEUR)) {
        case (_, user, _, _) =>
          val createPaymentRequest = CreatePaymentRequestParser.parse(request)

          for {
            // Validar la compra y obtener datos necesarios
            purchaseData <- paymentValidator.validatePurchase(createPaymentRequest.resourceId, user)

            // Chequear si la transacción ya está
            existingTransaction <-
              transactionRepository
                .find(
                  TransactionFilter(
                    buyerId = Some(user.id),
                    resourceId = Some(createPaymentRequest.resourceId),
                    transactionType = Some(TransactionType.PAYMENT)
                  ),
                  Pagination.single
                ).map(_.headOption)

            // Generar IDs
            transactionId = existingTransaction.map(_.id).getOrElse(TransactionId.gen(configuration.nodeId))

            // Crear Transaction PRIMERO (antes que el PaymentIntent)
            transaction = Transaction(
              id = transactionId,
              transactionType = TransactionType.PAYMENT,
              transactionStatus = TransactionStatus.PENDING,
              sellerId = purchaseData.seller.id,
              buyerId = user.id,
              resourceId = createPaymentRequest.resourceId,
              fee = Some(purchaseData.fee),
              created = clock.now,
              updated = clock.now,
              metadata = None,
              completedSuccessfullyAt = None,
              cancelledRejectedAt = None
            )

            // Guardar Transaction
            _ <- transactionRepository.save(transaction)

            requestFingerprint = getFingerprint
            // PaymentIntent (se llama a Stripe API aquí)
            // Se conecta aquí con Stripe.
            paymentIntent <- paymentCreator.createPaymentIntent(
              createPaymentRequest,
              user,
              transactionId,
              requestFingerprint,
              purchaseData
            )

            // Guardar PaymentIntent en BD
            _ <- paymentIntentRepository.save(paymentIntent).recoverWith {
              case NonFatal(e) =>
                // Si falla guardar el payment_intent, eliminar el Resource para no dejar datos huérfanos
                transactionRepository.delete(transactionId).map(_ => throw e)
            }

            // Generar respuesta JSON
            paymentIntentJs <- paymentIntentWriter.toJson(paymentIntent, transaction)

          } yield Ok(paymentIntentJs)
      }
    }

//Para obtener el estado de un PaymentIntent
  def getPaymentIntentStatus(paymentIntentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val pid = PaymentId(paymentIntentId)

          for {
            paymentIntent <-
              paymentIntentRepository
                .findByPaymentId(pid)
                .map(_.getOrElse(throw new PaymentNotFoundException(s"PaymentIntent ${paymentIntentId} not found")))

            // Obtener Transaction para verificar permisos
            transaction <-
              transactionRepository
                .findById(paymentIntent.transactionId)
                .map(_.getOrElse(throw new TransactionNotFoundException("Transaction not found")))

            // Verificar que el usuario es el comprador
            _ = if (transaction.buyerId != user.id) {
              throw new TransactionNotAllowedException("You are not authorized")
            }

            paymentIntent <- paymentIntentWriter.toJson(paymentIntent, transaction)

          } yield Ok(paymentIntent)
      }
    }

  //Se obtiene los pagos que ha realizado según el rol de participación en la transacción.
  def getMyPaymentsAs(pag: Pagination, paymentRole: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>

          val transactionFilter = paymentRole.toLowerCase match {
            case "buyer" =>
              TransactionFilter(
                buyerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )

            case "seller" =>
              TransactionFilter(
                sellerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )

            case _ =>
              TransactionFilter(
                buyerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )
          }

          for {
            transactions <- transactionRepository.find(transactionFilter, pag.oneMore)

            json <- paginatedWriter.toJson(Paginated(transactions, pag)) { transaction =>
              for {
                payment <- paymentRepository
                  .findByTransactionId(transaction.id)
                  .map(
                    _.getOrElse(
                      throw new PaymentNotFoundException(
                        s"Payment not found for transaction ${transaction.id}"
                      )
                    )
                  )

                json <- paymentRole.toLowerCase match {
                  case "seller" =>
                    paymentWriter.asSeller(transaction, payment)

                  case _ =>
                    paymentWriter.asBuyer(transaction, payment)
                }
              } yield json
            }
          } yield Ok(json)
      }
    }


  // Obtiene un pago específico por ID
  def getPaymentById(paymentId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val pid = PaymentId(paymentId)

          for {
            payment <-
              paymentRepository
                .findById(pid)
                .map(_.getOrElse(throw new PaymentNotFoundException("Payment not found")))

            // Obtener la transacción asociada para verificar permisos
            transaction <-
              transactionRepository
                .findById(payment.transactionId)
                .map(_.getOrElse(throw new TransactionNotFoundException("Associated transaction not found")))

            // Verificar que el usuario es el comprador o vendedor
            _ = if (transaction.buyerId != user.id && transaction.sellerId != user.id) {
              throw new TransactionNotAllowedException("Not authorized to view this transaction")
            }

            json <- paymentWriter.toJson(transaction, payment)

          } yield Ok(json)
      }
    }

  def getPaymentsSummary(paymentRole: String, fromDate: Option[String], toDate: Option[String]): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>

          val transactionFilter = paymentRole.toLowerCase match {
            case "buyer" =>
              TransactionFilter(
                buyerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )

            case "seller" =>
              TransactionFilter(
                sellerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )

            case _ =>
              TransactionFilter(
                buyerId = Some(user.id),
                transactionType = Some(TransactionType.PAYMENT),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              )
          }

          for {
            // Obtener todas las transacciones de pago del usuario
            transactions <- transactionRepository.find(transactionFilter, Pagination.all)

            // Filtrar por fechas si se proporcionan
            filteredTransactions: List[Transaction] = transactions.filter { transaction =>
              val isAfterFromDate = fromDate match {
                case Some(from) =>
                  try {
                    val fromDateTime = DateTime.parse(from)
                    transaction.created.isAfter(fromDateTime) || transaction.created.isEqual(fromDateTime)
                  } catch {
                    case _: Exception => true
                  }
                case None => true
              }

              val isBeforeToDate = toDate match {
                case Some(to) =>
                  try {
                    val toDateTime = DateTime.parse(to)
                    transaction.completedSuccessfullyAt.exists(completed => completed.isBefore(toDateTime.plusDays(1)))
                  } catch {
                    case _: Exception => true
                  }
                case None => true
              }
              isAfterFromDate && isBeforeToDate
            }

            // Obtener todos los pagos asociados
            payments <- Future.sequence(
              filteredTransactions.map { transaction =>
                paymentRepository.findByTransactionId(transaction.id)
              }
            )

            // Calcular el resumen
            summary = calculatePaymentsSummary(filteredTransactions, payments, paymentRole)

          } yield Ok(summary)
      }
    }

  private def calculatePaymentsSummary(transactions: List[Transaction], payments: List[Option[Payment]], paymentRole: String): json.JsValue = {
    val paymentsList = payments.flatten

    // Según el rol, usamos el campo correcto:
    // - Buyer: amount (lo que pagó)
    // - Seller: netAmount (lo que recibió)
    val isSeller = paymentRole.toLowerCase == "seller"

    val totalAmount = if (isSeller) {
      paymentsList.map(_.netAmount).sum  // Lo que recibió el vendedor
    } else {
      paymentsList.map(_.amount).sum     // Lo que pagó el comprador
    }

    val totalTransactions = paymentsList.length
    val averageAmount = if (totalTransactions > 0) totalAmount / totalTransactions else BigDecimal(0)

    Json.obj(
      "paymentRole" -> paymentRole,
      "totalTransactions" -> totalTransactions,
      "totalAmount" -> totalAmount.toString,
      "averageAmount" -> averageAmount.toString,
      "currency" -> "EUR",
      "transactionDetails" -> play.api.libs.json.Json.toJson(
        transactions.map { transaction =>
          val paymentOpt = paymentsList.find(_.transactionId == transaction.id)
          val amountValue = paymentOpt.map { payment =>
            if (isSeller) payment.netAmount.toString else payment.amount.toString
          }.getOrElse("0")

          Json.obj(
            "transactionId" -> transaction.id.toString,
            "amount" -> amountValue,
            "createdAt" -> transaction.created.toString,
            "status" -> transaction.transactionStatus.toString
          )
        }
      )
    )
  }
}
