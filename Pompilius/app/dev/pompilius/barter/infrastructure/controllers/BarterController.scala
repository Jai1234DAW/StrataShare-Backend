package dev.pompilius.barter.infrastructure.controllers

import dev.pompilius.Strings
import dev.pompilius.auth.domain.MailToken
import dev.pompilius.auth.infrastructure.parsers.MailTokenParser
import dev.pompilius.auth.infrastructure.writers.MailTokenWriter
import dev.pompilius.barter.domain.exception.{BarterAlreadyCompletedException, BarterNotAllowException, BarterNotFoundException}
import dev.pompilius.barter.domain.{Barter, BarterData, BarterId, BarterRepository}
import dev.pompilius.barter.infrastructure.parsers.{BarterRequestParser, CreateBarterRequestParser, MailBarterRequestParser}
import dev.pompilius.barter.infrastructure.writers.BarterWriter
import dev.pompilius.mail.domain.{Mail, MailAddress, MailContent, MailSubject}
import dev.pompilius.mail.infrastructure.repositories.MailSmtpRepository
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
import dev.pompilius.transaction.application.TransactionService
import dev.pompilius.transaction.domain._
import dev.pompilius.transaction.domain.exceptions.TransactionNotFoundException
import dev.pompilius.users.domain.Role
import play.api.Logger
import play.api.i18n.{Lang, Messages, MessagesImpl}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class BarterController @Inject() (
    barterRepository: BarterRepository,
    transactionRepository: TransactionRepository,
    transactionService: TransactionService,
    barterWriter: BarterWriter,
    mailTokenWriter: MailTokenWriter,
    mailSmtpRepository: MailSmtpRepository
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

  def create: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val createRequest = CreateBarterRequestParser.parse(request)

          val resourceId = ResourceId(createRequest.resourceId.id)
          val offeredResourceId = ResourceId(createRequest.offeredResourceId.id)

          for {
            // Validar que el trueque es posible (incluye todas las validaciones de negocio)
            data <- transactionService.isAbleToBarter(resourceId, user, offeredResourceId)

            // Generar IDs
            transactionId = TransactionId.gen(configuration.nodeId)
            barterId = BarterId.gen(configuration.nodeId)

            // Crear Transaction con estado (PENDING)
            transaction = Transaction(
              id = transactionId,
              transactionType = TransactionType.BARTER,
              transactionStatus = TransactionStatus.PENDING,
              sellerId = data.seller.id,
              buyerId = user.id,
              resourceId = resourceId,
              fee = Some(BigDecimal(0)), // Sin fee en trueques)
              created = clock.now,
              updated = clock.now,
              metadata = Some(Json.obj("offeredResourceId" -> offeredResourceId.toString).toString)
            )

            // Crear Barter asociado a la Transaction
            barter = Barter(
              barterId = barterId,
              transactionId = transactionId,
              offeredResourceId = offeredResourceId
            )

            // Guardar
            _ <- transactionRepository.save(transaction)
            _ <- barterRepository.save(barter).recoverWith {
              case NonFatal(e) =>
                // Si falla guardar el Barter, eliminar la Transaction para no dejar datos huérfanos
                transactionRepository.delete(transactionId).map(_ => throw e)
            }

            //Enviar notificación por email al vendedor
            _ <- sendBarterRequestMail(data, barterId, transactionId).recover {
              case e: Throwable =>
                // Log el error pero no falla la operación si el email falla
                logger.warn(s"Failed to send barter notification email: ${e.getMessage}")
                ()
            }

            json <- barterWriter.toJson(transaction, barter)

          } yield Ok(json)
      }
    }

  private def sendBarterRequestMail(
      data: BarterData,
      barterId: BarterId,
      transactionId: TransactionId
  ): Future[Unit] = {
    for {
      token <- mailTokenWriter.toString(
        MailToken(
          data.seller.email,
          clock.now.plusMillis(configuration.barter.requestLinkDuration.toMillis.toInt)
        ),
        configuration.mails.tokenSecretKey
      )

      messages = MessagesImpl(Lang("en"), messagesApi)

      link = UrlUtil.addQueryParameters(
        configuration.barter.purchaseResourceUrl,
        Map(
          Strings.token -> token,
          Strings.barterId -> barterId.toString,
          Strings.transactionId -> transactionId.toString,
          Strings.buyerId -> data.buyer.id.toString,
          Strings.requestedResourceId -> data.requestedResource.id.toString,
          Strings.offeredResourceId -> data.offeredResource.id.toString
        )
      )

      mailContent = dev.pompilius.barter.infrastructure.views.html.request_barter_email(
        sellerName = data.seller.username,
        buyerName = data.buyer.username,
        requestedResourceTitle = data.requestedResource.summary.getOrElse("A resource from your list of resources"),
        offeredResourceTitle = data.offeredResource.summary.getOrElse("Is offering a sample rock or a study"),
        link = link
      )(messages)

      mail = Mail(
        to = MailAddress(
          address = data.seller.email,
          name = Some(data.seller.username)
        ),
        subject = Some(MailSubject(messages("mail.barter.subject", data.buyer.username))),
        content = MailContent(
          text = None,
          html = Some(mailContent.body)
        )
      )

      _ <- mailSmtpRepository.sendMail(mail)
    } yield ()
  }

//  def acceptBarter: Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          val acceptRequest = MailBarterRequestParser.parse(request)
//
//          // Validar el token del email
//          val mailToken = MailTokenParser.parse(acceptRequest.token, configuration.mails.tokenSecretKey)
//
//          // Validar que el email del token coincide con el email enviado
//          if (!mailToken.mail.equalsIgnoreCase(acceptRequest.email)) {
//            throw new BadRequestException("The email does not match the token")
//          }
//
//          // Validar que el email coincide con el usuario logueado
//          if (!acceptRequest.email.equalsIgnoreCase(user.email)) {
//            throw new BadRequestException("The email does not match your account")
//          }
//
//          if (mailToken.expires.isBefore(clock.now)) {
//            throw new BadRequestException("Token expired")
//          }
//
//          val idB = BarterId(acceptRequest.barterId)
//          val idT = TransactionId(acceptRequest.transactionId)
//
//          for {
//            transaction <-
//              transactionRepository
//                .findById(idT)
//                .map(_.getOrElse(throw new BadRequestException("Transaction not found")))
//
//            barter <-
//              barterRepository
//                .findByTransactionId(idT)
//                .map(_.getOrElse(throw new BadRequestException("Barter not found")))
//
//            _ = if (barter.barterId != idB) {
//              throw new BarterNotAllowException("Barter ID does not match Transaction")
//            }
//
//            // Validar que el usuario es el vendedor
//            _ = if (transaction.sellerId != user.id) {
//              throw new BarterNotAllowException("Only the seller can accept the barter")
//            }
//
//            /// Validar que el trueque está pendiente
//            _ = transaction.transactionStatus match {
//
//              case TransactionStatus.PENDING =>
//                () // OK, se puede rechazar
//
//              case TransactionStatus.CANCELLED =>
//                throw new BarterNotAllowException(
//                  "Barter was cancelled by the buyer and cannot be accepted"
//                )
//
//              case _ =>
//                throw new BarterAlreadyCompletedException(
//                  "Only pending barters can be accepted"
//                )
//            }
//
//            // Transferir recursos (en transacción atómica)
//            _ <- transactionService.transactionTrans(transaction, barter)
//
//            json <- barterWriter.asSeller(transaction, barter)
//
//          } yield Ok(json)
//      }
//    }

  def acceptBarter: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val acceptRequest = BarterRequestParser.parse(request)

          val idB = BarterId(acceptRequest.barterId)
          val idT = TransactionId(acceptRequest.transactionId)

          for {
            transaction <-
              transactionRepository
                .findById(idT)
                .map(_.getOrElse(throw new BadRequestException("Transaction not found")))

            barter <-
              barterRepository
                .findByTransactionId(idT)
                .map(_.getOrElse(throw new BadRequestException("Barter not found")))

            _ = if (barter.barterId != idB) {
              throw new BarterNotAllowException("Barter ID does not match Transaction")
            }

            // Validar que el usuario es el vendedor
            _ = if (transaction.sellerId != user.id) {
              throw new BarterNotAllowException("Only the seller can accept the barter")
            }

            /// Validar que el trueque está pendiente
            _ = transaction.transactionStatus match {

              case TransactionStatus.PENDING =>
                () // OK, se puede rechazar

              case TransactionStatus.CANCELLED =>
                throw new BarterNotAllowException(
                  "Barter was cancelled by the buyer and cannot be accepted"
                )

              case _ =>
                throw new BarterAlreadyCompletedException(
                  "Only pending barters can be accepted"
                )
            }

            // Transferir recursos (en transacción atómica)
            _ <- transactionService.transactionTrans(transaction, barter)

            json <- barterWriter.asSeller(transaction, barter)

          } yield Ok(json)
      }
    }

  def denyBarter: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          val denyRequest = BarterRequestParser.parse(request)

          val idB = BarterId(denyRequest.barterId)
          val idT = TransactionId(denyRequest.transactionId)

          for {
            transaction <-
              transactionRepository
                .findById(idT)
                .map(_.getOrElse(throw new BadRequestException("Transaction not found")))

            barter <-
              barterRepository
                .findByTransactionId(idT)
                .map(_.getOrElse(throw new BadRequestException("Barter not found")))

            _ = if (barter.barterId != idB) {
              throw new BarterNotAllowException("Barter ID does not match Transaction")
            }

            // Validar que el usuario es el vendedor
            _ = if (transaction.sellerId != user.id) {
              throw new BarterNotAllowException("Only the seller can reject the barter")
            }

            // Validar que el trueque está pendiente
            _ = transaction.transactionStatus match {

              case TransactionStatus.PENDING =>
                () // OK, se puede rechazar

              case TransactionStatus.CANCELLED =>
                throw new BarterNotAllowException(
                  "Barter was cancelled by the buyer and cannot be rejected"
                )

              case _ =>
                throw new BarterAlreadyCompletedException(
                  "Only pending barters can be rejected"
                )
            }
//            // Marcar el trueque como rechazado
//            updatedBarter = barter.copy(rejectedAt = Some(clock.now))
//            _ <- barterRepository.save(updatedBarter)

            // Actualizar transaction a REJECTED
            _ <- transactionRepository.updateStatusRejectedCancelled(idT, TransactionStatus.REJECTED)

          } yield Ok
      }
    }

//  def denyBarter: Action[AnyContent] =
//    Action.async { implicit request =>
//      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
//        case (_, user, _, _) =>
//          val denyRequest = MailBarterRequestParser.parse(request)
//
//          // Validar el token del email
//          val mailToken = MailTokenParser.parse(denyRequest.token, configuration.mails.tokenSecretKey)
//
//          if (!mailToken.mail.equalsIgnoreCase(user.email)) {
//            throw new BadRequestException("The email token does not match your account")
//          }
//
//          if (mailToken.expires.isBefore(clock.now)) {
//            throw new BadRequestException("Token expired")
//          }
//
//          val idB = BarterId(denyRequest.barterId)
//          val idT = TransactionId(denyRequest.transactionId)
//
//          for {
//            transaction <-
//              transactionRepository
//                .findById(idT)
//                .map(_.getOrElse(throw new BadRequestException("Transaction not found")))
//
//            barter <-
//              barterRepository
//                .findByTransactionId(idT)
//                .map(_.getOrElse(throw new BadRequestException("Barter not found")))
//
//            _ = if (barter.barterId != idB) {
//              throw new BarterNotAllowException("Barter ID does not match Transaction")
//            }
//
//            // Validar que el usuario es el vendedor
//            _ = if (transaction.sellerId != user.id) {
//              throw new BarterNotAllowException("Only the seller can reject the barter")
//            }
//
//            // Validar que el trueque está pendiente
//            _ = transaction.transactionStatus match {
//
//              case TransactionStatus.PENDING =>
//                () // OK, se puede rechazar
//
//              case TransactionStatus.CANCELLED =>
//                throw new BarterNotAllowException(
//                  "Barter was cancelled by the buyer and cannot be rejected"
//                )
//
//              case _ =>
//                throw new BarterAlreadyCompletedException(
//                  "Only pending barters can be rejected"
//                )
//            }
//            //            // Marcar el trueque como rechazado
//            //            updatedBarter = barter.copy(rejectedAt = Some(clock.now))
//            //            _ <- barterRepository.save(updatedBarter)
//
//            // Actualizar transaction a REJECTED
//            _ <- transactionRepository.updateStatusRejectedCancelled(idT, TransactionStatus.REJECTED)
//
//          } yield Ok
//      }
//    }

  //Esto es para cancelar un trueque antes de que el vendedor lo acepte.
  def cancel(transactionId: String, barterId: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val eid = TransactionId(transactionId)
          val bId = BarterId(barterId)

          for {
            transaction <-
              transactionRepository
                .findById(eid)
                .map(_.getOrElse(throw new TransactionNotFoundException("Transaction not found")))

            barter <-
              barterRepository
                .findByTransactionId(eid)
                .map(_.getOrElse(throw new BarterNotFoundException("Barter not found")))

            _ = if (barter.barterId != bId) {
              throw new BarterNotFoundException("Barter ID does not match Transaction")
            }

            // Validar que el usuario es el comprador
            _ = if (transaction.buyerId != user.id) {
              throw new BarterNotAllowException("Only the buyer can cancel the barter")
            }

            // Validar que el trueque está pendiente
            _ = if (transaction.transactionStatus != TransactionStatus.PENDING) {
              throw new BarterNotAllowException("Only pending barters can be cancelled")
            }

            // Actualizar Transaction a CANCELLED
            _ <- transactionRepository.updateStatusRejectedCancelled(transaction.id, TransactionStatus.CANCELLED)
            //Se puede enviar una notificacion al vendedor, pero esto no es critico

          } yield Ok(Json.obj("message" -> "Barter cancelled successfully"))
      }
    }

  def getMyRequestSuccessfulBarters(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            transactions <- transactionRepository.find(
              TransactionFilter(
                buyerId = Some(user.id),
                sellerId = None,
                resourceId = None,
                transactionType = Some(TransactionType.BARTER),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              ),
              pag.oneMore
            )

            bartersWithTransactions <- Future.sequence(
              transactions.map { transaction =>
                barterRepository
                  .findByTransactionId(transaction.id)
                  .map(_.get)
                  .map(barter => transaction -> barter)
              }
            )

            jsons <- Future.sequence(
              bartersWithTransactions.map {
                case (transaction, barter) =>
                  barterWriter.asBuyer(transaction, barter)
              }
            )
          } yield Ok(Json.toJson(jsons))
      }
    }

  def getMySalesSuccessfulBarters(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            transactions <- transactionRepository.find(
              TransactionFilter(
                buyerId = None,
                sellerId = Some(user.id),
                resourceId = None,
                transactionType = Some(TransactionType.BARTER),
                transactionStatus = Some(TransactionStatus.COMPLETED)
              ),
              pag.oneMore
            )

            bartersWithTransactions <- Future.sequence(
              transactions.map { transaction =>
                barterRepository
                  .findByTransactionId(transaction.id)
                  .map(_.get)
                  .map(barter => transaction -> barter)
              }
            )

            jsons <- Future.sequence(
              bartersWithTransactions.map {
                case (transaction, barter) =>
                  barterWriter.asSeller(transaction, barter)
              }
            )
          } yield Ok(Json.toJson(jsons))
      }
    }

  def getMyPendingBarters(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            transactions <- transactionRepository.find(
              TransactionFilter(
                buyerId = Some(user.id),
                sellerId = None,
                resourceId = None,
                transactionType = Some(TransactionType.BARTER),
                transactionStatus = Some(TransactionStatus.PENDING)
              ),
              pag.oneMore
            )

            bartersWithTransactions <- Future.sequence(
              transactions.map { transaction =>
                barterRepository
                  .findByTransactionId(transaction.id)
                  .map { optionBarter =>
                    optionBarter.map(barter => transaction -> barter)
                  }
                  .map(_.get)  // Aquí sí es seguro porque validaste arriba
              }
            )

            jsons <- Future.sequence(
              bartersWithTransactions.map {
                case (transaction, barter) =>
                  barterWriter.asBuyer(transaction, barter)
              }
            )
          } yield Ok(Json.toJson(jsons))
      }
    }

  def getMyPendingBartersToAnswer(pag: Pagination): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.STUDENT, Role.PROFESSIONAL)) {
        case (_, user, _, _) =>
          for {
            transactions <- transactionRepository.find(
              TransactionFilter(
                buyerId = None,
                sellerId = Some(user.id),
                resourceId = None,
                transactionType = Some(TransactionType.BARTER),
                transactionStatus = Some(TransactionStatus.PENDING)
              ),
              pag.oneMore
            )


            bartersWithTransactions <- Future.sequence(
              transactions.map { transaction =>
                barterRepository
                  .findByTransactionId(transaction.id)
                  .map { optionBarter =>
                    optionBarter.map(barter => transaction -> barter)
                  }
                  .map(_.get)
              }
            )

            jsons <- Future.sequence(
              bartersWithTransactions.map {
                case (transaction, barter) =>
                  barterWriter.asSeller(transaction, barter)
              }
            )
          } yield Ok(Json.toJson(jsons))
      }
    }
}
