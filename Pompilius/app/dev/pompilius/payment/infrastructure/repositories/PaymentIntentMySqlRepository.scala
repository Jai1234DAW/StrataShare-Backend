package dev.pompilius.payment.infrastructure.repositories

import dev.pompilius.payment.domain.{Gateway, PaymentId, PaymentIntent, PaymentIntentRepository, PaymentIntentStatus}
import dev.pompilius.shared.domain.Clock
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import org.joda.time.DateTime
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PaymentIntentMySqlRepository @Inject() (clock: Clock)(implicit dbExecutionContext: DbExecutionContext)
    extends PaymentIntentRepository
    with SQLSyntaxSupport[PaymentIntent] {

  override val tableName = "payment_intent"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(pi: SyntaxProvider[PaymentIntent])(rs: WrappedResultSet): PaymentIntent =
    apply(pi.resultName)(rs)

  def apply(pi: ResultName[PaymentIntent])(rs: WrappedResultSet): PaymentIntent =
    PaymentIntent(
      paymentId = PaymentId(rs.get[Long](pi.paymentId)),
      gateway = Gateway.withNameInsensitive(rs.get[String](pi.gateway)),
      gatewayIntentId = rs.get[String](pi.gatewayIntentId),
      amount = rs.get[BigDecimal](pi.amount),
      currency = rs.get[String](pi.currency),
      status = PaymentIntentStatus.withName(rs.get[String](pi.status)),
      created = rs.get[DateTime](pi.created),
      updated = rs.get[DateTime](pi.updated),
      fingerprint = rs.get[String](pi.fingerprint),
      metadata = rs.get[Option[String]](pi.metadata)
    )

  private val pi = this.syntax("pi")

  override def create(paymentIntent: PaymentIntent): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.paymentId -> paymentIntent.paymentId.id,
          column.gateway -> paymentIntent.gateway.toString,
          column.gatewayIntentId -> paymentIntent.gatewayIntentId,
          column.amount -> paymentIntent.amount,
          column.currency -> paymentIntent.currency,
          column.status -> paymentIntent.status.toString,
          column.created -> paymentIntent.created,
          column.updated -> paymentIntent.updated,
          column.fingerprint -> paymentIntent.fingerprint,
          column.metadata -> paymentIntent.metadata
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.paymentId, values: _*))
        }.update()
      }
      Done
    }

  override def save(paymentIntent: PaymentIntent): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.gateway -> paymentIntent.gateway.toString,
              column.gatewayIntentId -> paymentIntent.gatewayIntentId,
              column.amount -> paymentIntent.amount,
              column.currency -> paymentIntent.currency,
              column.status -> paymentIntent.status.toString,
              sqls"updated" -> clock.now,
              column.fingerprint -> paymentIntent.fingerprint,
              column.metadata -> paymentIntent.metadata
            )
            .where
            .eq(column.paymentId, paymentIntent.paymentId.id)
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

}
