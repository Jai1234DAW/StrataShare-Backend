package dev.pompilius.payment.infrastructure.repositories

import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.payment.domain.{PaymentId, PaymentIntent, PaymentIntentRepository, PaymentIntentStatus}
import dev.pompilius.shared.domain.Clock
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.transaction.domain.TransactionId
import org.apache.pekko.Done
import org.joda.time.DateTime
import play.api.libs.json.Json
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.util.Try

@Singleton
class PaymentIntentMySqlRepository @Inject() (clock: Clock)(implicit dbExecutionContext: DbExecutionContext)
    extends PaymentIntentRepository
    with SQLSyntaxSupport[PaymentIntent] {

  override val tableName = "payment_intent"
  // Composite key support: (paymentId, transactionId)
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(pi: SyntaxProvider[PaymentIntent])(rs: WrappedResultSet): PaymentIntent =
    apply(pi.resultName)(rs)

  def apply(pi: ResultName[PaymentIntent])(rs: WrappedResultSet): PaymentIntent =
    PaymentIntent(
      paymentId = PaymentId(rs.get[Long](pi.paymentId)),
      transactionId = TransactionId(rs.get[Long](pi.transactionId)),
      gateway = Gateway.withNameInsensitive(rs.get[String](pi.gateway)),
      gatewayIntentId = rs.get[String](pi.gatewayIntentId),
      price = rs.get[BigDecimal](pi.price),
      surcharge = rs.get[BigDecimal](pi.surcharge),
      amount = rs.get[BigDecimal](pi.amount),
      status = PaymentIntentStatus.withNameInsensitive(rs.get[String](pi.status)),
      //couponCode = rs.get[Option[String]](pi.couponCode),
      discount = rs.get[Option[BigDecimal]](pi.discount),
      url = rs.get[Option[String]](pi.url),
      created = rs.get[DateTime](pi.created),
      buyerReference = rs.get[Option[String]](pi.buyerReference),
      instrument = rs.get[Option[String]](pi.instrument),
      fingerprint = rs.get[Option[String]](pi.fingerprint),
      returnUrlParams = rs.get[Option[String]](pi.returnUrlParams).flatMap { js =>
        Try(Json.parse(js).as[Map[String, String]]).toOption
      },
      metadata = rs.get[Option[String]](pi.metadata),
      extraInfo = rs.get[Option[String]](pi.extraInfo),
      updated = rs.get[Option[DateTime]](pi.updated)
    )

  private val pi = this.syntax("pi")

  override def save(paymentIntent: PaymentIntent): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.paymentId -> paymentIntent.paymentId.id,
          column.transactionId -> paymentIntent.transactionId.id,
          column.gateway -> paymentIntent.gateway.toString,
          column.gatewayIntentId -> paymentIntent.gatewayIntentId,
          column.price -> paymentIntent.price,
          column.surcharge -> paymentIntent.surcharge,
          column.amount -> paymentIntent.amount,
          column.status -> paymentIntent.status.toString,
          //column.couponCode -> paymentIntent.couponCode,
          column.discount -> paymentIntent.discount,
          column.url -> paymentIntent.url,
          column.created -> paymentIntent.created,
          column.buyerReference -> paymentIntent.buyerReference,
          column.instrument -> paymentIntent.instrument,
          column.fingerprint -> paymentIntent.fingerprint,
          column.returnUrlParams -> Try(Json.toJson(paymentIntent.returnUrlParams).toString).toOption,
          column.metadata -> paymentIntent.metadata,
          column.extraInfo -> paymentIntent.extraInfo,
          column.updated -> clock.now
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.paymentId, column.transactionId, values: _*))
        }.update()
      }
      Done
    }

  override def findByPaymentId(paymentId: PaymentId): Future[Option[PaymentIntent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as pi).where.eq(pi.paymentId, paymentId.id)
        }.map(apply(pi.resultName)(_)).single()
      }
    }

  override def findByGatewayIntentId(gateway: Gateway, gatewayIntentId: String): Future[Option[PaymentIntent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as pi).where
            .eq(pi.gatewayIntentId, gatewayIntentId)
            .and
            .eq(pi.gateway, gateway.toString)
        }.map(apply(pi.resultName)(_)).single()
      }
    }

  override def updateStatus(paymentId: PaymentId, status: PaymentIntentStatus): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.status -> status.toString,
              column.updated -> DateTime.now()
            )
            .where
            .eq(column.paymentId, paymentId.id)
        }.update()
      }
      Done
    }

    override def markSucceededIfNotSucceeded(paymentId: PaymentId): Future[Int] =
      Future {
        DB.localTx { implicit session =>
          withSQL {
            update(this)
              .set(
                column.status -> PaymentIntentStatus.SUCCEEDED.toString,
                column.updated -> DateTime.now()
              )
              .where
              .eq(column.paymentId, paymentId.id)
          }.update()
        }
      }

  override def findByPaymentIdAndTransactionId(
      paymentId: PaymentId,
      transactionId: TransactionId
  ): Future[Option[PaymentIntent]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as pi).where
            .eq(pi.paymentId, paymentId.id)
            .and
            .eq(pi.transactionId, transactionId.id)
        }.map(apply(pi.resultName)(_)).single()
      }
    }

  override def updateStatusByCompositeKey(
      paymentId: PaymentId,
      transactionId: TransactionId,
      status: PaymentIntentStatus
  ): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.status -> status.toString,
              column.updated -> DateTime.now()
            )
            .where
            .eq(column.paymentId, paymentId.id)
            .and
            .eq(column.transactionId, transactionId.id)
        }.update()
      }
      Done
    }

  override def markSucceededIfNotSucceededByCompositeKey(
      paymentId: PaymentId,
      transactionId: TransactionId
  ): Future[Int] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.status -> PaymentIntentStatus.SUCCEEDED.toString,
              column.updated -> DateTime.now()
            )
            .where
            .eq(column.paymentId, paymentId.id)
            .and
            .eq(column.transactionId, transactionId.id)
        }.update()
      }
    }
 }
