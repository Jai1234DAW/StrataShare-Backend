package dev.pompilius.barter.infrastructure.controllers

import dev.pompilius.Strings
import dev.pompilius.auth.domain.MailToken
import dev.pompilius.auth.infrastructure.writers.MailTokenWriter
import dev.pompilius.barter.domain.{Barter, BarterData, BarterId, BarterRepository}
import dev.pompilius.barter.infrastructure.parsers.CreateBarterRequestParser
import dev.pompilius.barter.infrastructure.writers.BarterWriter
import dev.pompilius.mail.domain.{Mail, MailAddress, MailContent, MailSubject}
import dev.pompilius.mail.infrastructure.repositories.MailSmtpRepository
import dev.pompilius.payment.domain.Gateway
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.infrastructure.{BaseController, UrlUtil}
import dev.pompilius.transaction.domain._
import dev.pompilius.transaction.infrastructure.TransactionValidator
import dev.pompilius.users.domain.Role
import play.api.Logger
import play.api.i18n.{Lang, MessagesImpl}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class BarterController @Inject() (
    barterRepository: BarterRepository,
    transactionRepository: TransactionRepository,
    transactionValidator: TransactionValidator,
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
            data <- transactionValidator.isAbleToBarter(resourceId, user, offeredResourceId)

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
              fee = BigDecimal(0), // Sin fee en trueques
              created = clock.now,
              updated = clock.now,
              metadata = Some(Json.obj("offeredResourceId" -> offeredResourceId.toString).toString)
            )

            // Crear Barter asociado a la Transaction
            barter = Barter(
              barterId = barterId,
              transactionId = transactionId,
              offeredResourceId = offeredResourceId,
              rejectedAt = None
            )

            // Guardar
            _ <- transactionRepository.save(transaction)
            _ <- barterRepository.save(barter).recoverWith {
              case NonFatal(e) =>
                // Si falla guardar el Barter, eliminar la Transaction para no dejar datos huérfanos
                transactionRepository.delete(transactionId).map(_ => throw e)
            }

            // Enviar notificación por email al vendedor
//            _ <- sendBarterRequestMail(data).recover {
//              case e: Throwable =>
//                // Log el error pero no falla la operación si el email falla
//                logger.warn(s"Failed to send barter notification email: ${e.getMessage}")
//                ()
//            }

            json <- barterWriter.asJson(barter, transaction, data.requestedResource)

          } yield Ok(json)
      }
    }

  private def sendBarterRequestMail(
      data: BarterData
  ): Future[Unit] = {
    for {
      token <- mailTokenWriter.toString(
        MailToken(
          data.seller.email,
          clock.now.plusMillis(configuration.auth.resetLinkDuration.toMillis.toInt)
        ),
        configuration.mails.tokenSecretKey
      )

      messages = MessagesImpl(Lang("en"), messagesApi)

      link = UrlUtil.addQueryParameters(
        configuration.barter.purchaseResourceUrl,
        Map(
          Strings.token -> token,
          "buyerId" -> data.buyer.id.toString,
          "requestedResourceId" -> data.requestedResource.id.toString,
          "offeredResourceId" -> data.offeredResource.id.toString
        )
      )

      mailContent = dev.pompilius.barter.infrastructure.views.html.request_barter_email(
        sellerName = data.seller.username,
        buyerName = data.buyer.username,
        requestedResourceTitle = data.requestedResource.summary.getOrElse(""),
        offeredResourceTitle = data.offeredResource.summary.getOrElse(""),
        link = link
      )(messages)

      mail = Mail(
        to = MailAddress(
          address = data.seller.email,
          name = None
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
}
