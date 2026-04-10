package dev.pompilius.payment.infrastructure.repositories

import dev.pompilius.payment.domain._
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.transaction.domain.TransactionId
import org.apache.pekko.Done
import org.joda.time.DateTime
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PaymentMySqlRepository @Inject() ()(implicit dbExecutionContext: DbExecutionContext)
    extends PaymentRepository
    with SQLSyntaxSupport[Payment] {

  override val tableName = "payment"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(p: SyntaxProvider[Payment])(rs: WrappedResultSet): Payment =
    apply(p.resultName)(rs)

  def apply(p: ResultName[Payment])(rs: WrappedResultSet): Payment =
    Payment(
      id = PaymentId(rs.get[Long](p.id)),
      transactionId = TransactionId(rs.get[Long](p.transactionId)),
      gateway = Gateway.withNameInsensitive(rs.get[String](p.gateway)),
      gatewayPaymentId = rs.get[String](p.gatewayPaymentId),
      price = rs.get[BigDecimal](p.price),
      amount = rs.get[BigDecimal](p.amount),
      discount = rs.get[BigDecimal](p.discount),
      netAmount = rs.get[BigDecimal](p.netAmount),
      currency = rs.get[String](p.currency),
      exchangeRate = rs.get[BigDecimal](p.exchangeRate),
      couponCode = rs.get[Option[String]](p.couponCode),
      buyerReference = rs.get[Option[String]](p.buyerReference),
      instrument = rs.get[Option[String]](p.instrument),
      receiptUrl = rs.get[Option[String]](p.receiptUrl),
      refunded = rs.get[Boolean](p.refunded),
      created = rs.get[DateTime](p.created),
      updated = rs.get[DateTime](p.updated),
      metadata = rs.get[Option[String]](p.metadata)
    )

  private val p = this.syntax("p")

  override def create(payment: Payment): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> payment.id.id,
          column.transactionId -> payment.transactionId.id,
          column.gateway -> payment.gateway.toString,
          column.gatewayPaymentId -> payment.gatewayPaymentId,
          column.price -> payment.price,
          column.amount -> payment.amount,
          column.discount -> payment.discount,
          column.netAmount -> payment.netAmount,
          column.currency -> payment.currency,
          column.exchangeRate -> payment.exchangeRate,
          column.couponCode -> payment.couponCode,
          column.buyerReference -> payment.buyerReference,
          column.instrument -> payment.instrument,
          column.receiptUrl -> payment.receiptUrl,
          column.refunded -> payment.refunded,
          column.created -> payment.created,
          column.updated -> payment.updated,
          column.metadata -> payment.metadata
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.id, values: _*))
        }.update()
      }
      Done
    }

  override def findByTransactionId(transactionId: TransactionId): Future[Option[Payment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as p).where.eq(p.transactionId, transactionId.id)
        }.map(apply(p.resultName)(_)).single()
      }
    }

  override def findById(id: PaymentId): Future[Option[Payment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as p).where.eq(p.id, id.id)
        }.map(apply(p.resultName)(_)).single()
      }
    }

  override def findByGatewayPaymentId(gateway: String, gatewayPaymentId: String): Future[Option[Payment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as p).where
            .eq(p.gateway, gateway)
            .and
            .eq(p.gatewayPaymentId, gatewayPaymentId)
        }.map(apply(p.resultName)(_)).single()
      }
    }

  override def find(filter: PaymentFilter, pag: Pagination): Future[List[Payment]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as p)
            .append(buildFilter(filter).getOrElse(sqls.empty))
            .orderBy(p.created)
            .desc
            .append(ScalikeUtil.pag(pag))
        }.map(apply(p.resultName)(_)).list()
      }
    }

  private def buildFilter(filter: PaymentFilter): Option[SQLSyntax] = {
    val transactionFilter = filter.transactionId.map(tid => sqls.eq(p.transactionId, tid.id))
    val gatewayFilter = filter.gateway.map(g => sqls.eq(p.gateway, g.toString))
    val gatewayPaymentIdFilter = filter.gatewayPaymentId.map(id => sqls.eq(p.gatewayPaymentId, id))
    val currencyFilter = filter.currency.map(c => sqls.eq(p.currency, c))
    val instrumentFilter = filter.instrument.map(i => sqls.eq(p.instrument, i))
    val couponCodeFilter = filter.couponCode.map(c => sqls.eq(p.couponCode, c))
    val refundedFilter = filter.refunded.map(r => sqls.eq(p.refunded, r))
    val withDiscountFilter = filter.withDiscount.map { wd =>
      if (wd) sqls.gt(p.discount, BigDecimal(0))
      else sqls.eq(p.discount, BigDecimal(0))
    }
    val minAmountFilter = filter.minAmount.map(min => sqls.ge(p.amount, min))
    val maxAmountFilter = filter.maxAmount.map(max => sqls.le(p.amount, max))

    val filters = List(
      transactionFilter,
      gatewayFilter,
      gatewayPaymentIdFilter,
      currencyFilter,
      instrumentFilter,
      couponCodeFilter,
      refundedFilter,
      withDiscountFilter,
      minAmountFilter,
      maxAmountFilter
    ).flatten

    filters match {
      case Nil  => None
      case list => Some(sqls.where(sqls.joinWithAnd(list: _*)))
    }
  }

}
