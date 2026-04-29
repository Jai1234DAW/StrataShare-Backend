package dev.pompilius.transaction.infrastructure.repositories

import dev.pompilius.shared.domain.{Clock, Pagination}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.transaction.domain.{
  Transaction,
  TransactionFilter,
  TransactionId,
  TransactionRepository,
  TransactionStatus,
  TransactionType
}
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import org.joda.time.DateTime
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class TransactionMySqlRepository @Inject() (clock: Clock)(implicit dbExecutionContext: DbExecutionContext)
    extends TransactionRepository
    with SQLSyntaxSupport[Transaction] {

  override val tableName = "transaction"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(t: SyntaxProvider[Transaction])(rs: WrappedResultSet): Transaction =
    apply(t.resultName)(rs)

  def apply(t: ResultName[Transaction])(rs: WrappedResultSet): Transaction =
    Transaction(
      id = TransactionId(rs.get[Long](t.id)),
      transactionType = TransactionType.withName(rs.get[String](t.transactionType)),
      transactionStatus = TransactionStatus.withName(rs.get[String](t.transactionStatus)),
      sellerId = UserId(rs.get[Long](t.sellerId)),
      buyerId = UserId(rs.get[Long](t.buyerId)),
      resourceId = ResourceId(rs.get[Long](t.resourceId)),
      fee = rs.getOpt[BigDecimal](t.fee),
      created = rs.get[DateTime](t.created),
      updated = rs.get[DateTime](t.updated),
      metadata = rs.get[Option[String]](t.metadata),
      completedSuccessfullyAt = rs.get[Option[DateTime]](t.completedSuccessfullyAt),
      cancelledRejectedAt = rs.get[Option[DateTime]](t.cancelledRejectedAt)
    )

  private val t = this.syntax("t")

  override def save(transaction: Transaction): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> transaction.id.id,
          column.transactionType -> transaction.transactionType.toString,
          column.transactionStatus -> transaction.transactionStatus.toString,
          column.sellerId -> transaction.sellerId.id,
          column.buyerId -> transaction.buyerId.id,
          column.resourceId -> transaction.resourceId.id,
          column.fee -> transaction.fee,
          column.created -> transaction.created,
          column.updated -> transaction.updated,
          column.metadata -> transaction.metadata,
          column.completedSuccessfullyAt -> transaction.completedSuccessfullyAt,
          column.cancelledRejectedAt -> transaction.cancelledRejectedAt
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(values: _*))
        }.update()
      }
      Done
    }

  override def findById(id: TransactionId): Future[Option[Transaction]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as t).where.eq(t.id, id.id)
        }.map(apply(t.resultName)(_)).single()
      }
    }

  override def find(filter: TransactionFilter, pag: Pagination): Future[List[Transaction]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as t)
            .append(filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty))
            .orderBy(t.created)
            .desc
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(t.resultName)(_)).list()
      }
    }

  private def filterToSqlSyntax(filter: TransactionFilter): Option[SQLSyntax] = {
    val filters = List(
      filter.buyerId.map(buyerId => sqls.eq(t.buyerId, buyerId.id)),
      filter.sellerId.map(sellerId => sqls.eq(t.sellerId, sellerId.id)),
      filter.resourceId.map(resourceId => sqls.eq(t.resourceId, resourceId.id)),
      filter.transactionType.map(transactionType => sqls.eq(t.transactionType, transactionType.toString)),
      filter.transactionStatus.map(transactionStatus => sqls.eq(t.transactionStatus, transactionStatus.toString))
    ).flatten

    if (filters.nonEmpty) {
      Some(sqls.joinWithAnd(filters: _*))
    } else {
      None
    }
  }

  override def updateStatusCompleted(id: TransactionId, status: TransactionStatus): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.transactionStatus -> status.toString,
              column.updated -> clock.now,
              column.completedSuccessfullyAt -> clock.now
            )
            .where
            .eq(column.id, id.id)
        }.update()
      }
      Done
    }

  override def updateStatusRejectedCancelled(id: TransactionId, status: TransactionStatus): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          update(this)
            .set(
              column.transactionStatus -> status.toString,
              column.updated -> clock.now,
              column.cancelledRejectedAt -> clock.now
            )
            .where
            .eq(column.id, id.id)
        }.update()
      }
      Done
    }

  override def delete(id: TransactionId): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          deleteFrom(this).where.eq(column.id, id.id)
        }.update()
      }
      Done
    }
}
