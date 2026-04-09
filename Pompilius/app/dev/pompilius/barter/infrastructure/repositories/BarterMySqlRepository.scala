package dev.pompilius.barter.infrastructure.repositories

import dev.pompilius.barter.domain.{Barter, BarterFilter, BarterId, BarterRepository}
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.transaction.domain.TransactionId
import dev.pompilius.transaction.infrastructure.repositories.TransactionMySqlRepository
import org.apache.pekko.Done
import org.joda.time.DateTime
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._
import scalikejdbc.jodatime.JodaTypeBinder._

import java.time.ZoneId
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class BarterMySqlRepository @Inject() (
    transactionMySqlRepository: TransactionMySqlRepository
)(implicit dbExecutionContext: DbExecutionContext)
    extends BarterRepository
    with SQLSyntaxSupport[Barter] {

  override val tableName = "barter"
  implicit val overwrittenZoneId: OverwrittenZoneId = OverwrittenZoneId(ZoneId.of("UTC"))

  def apply(b: SyntaxProvider[Barter])(rs: WrappedResultSet): Barter =
    apply(b.resultName)(rs)

  def apply(b: ResultName[Barter])(rs: WrappedResultSet): Barter =
    Barter(
      barterId = BarterId(rs.get[Long](b.barterId)),
      transactionId = TransactionId(rs.get[Long](b.transactionId)),
      offeredResourceId = ResourceId(rs.get[Long](b.offeredResourceId)),
      rejectedAt = rs.get[Option[DateTime]](b.rejectedAt)
    )

  private val b = this.syntax("b")

  override def save(barter: Barter): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.barterId -> barter.barterId.id,
          column.transactionId -> barter.transactionId.id,
          column.offeredResourceId -> barter.offeredResourceId.id,
          column.rejectedAt -> barter.rejectedAt
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(column.barterId, values: _*))
        }.update()
      }
      Done
    }

  override def findByTransactionId(transactionId: TransactionId): Future[Option[Barter]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as b).where.eq(b.transactionId, transactionId.id)
        }.map(apply(b.resultName)(_)).single()
      }
    }

  override def find(filter: BarterFilter, pag: Pagination): Future[List[Barter]] =
    Future {
      val orderBy: Seq[SQLSyntax] = buildOrderBy(pag)

      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as b)
            .append(
              filterToSqlSyntax(filter).map(sqls.where(_)).getOrElse(sqls.empty)
            )
            .orderBy(orderBy: _*)
            .append(
              ScalikeUtil.pag(pag)
            )
        }.map(apply(b.resultName)(_)).list()
      }
    }

  private def filterToSqlSyntax(filter: BarterFilter): Option[SQLSyntax] = {

    val transactionId = filter.transactionId.map(tid => sqls.eq(b.transactionId, tid.id))

    val offeredResourceId = filter.offeredResourceId.map(rid => sqls.eq(b.offeredResourceId, rid.id))

    val rejected = filter.rejected.map { rejected =>
      if (rejected) sqls.isNotNull(b.rejectedAt)
      else sqls.isNull(b.rejectedAt)
    }

    val userBuyer = filter.userBuyer.map { buyerId =>
      val t = transactionMySqlRepository.syntax("t")
      sqls.exists(
        select(sqls"1")
          .from(transactionMySqlRepository as t)
          .where
          .eq(t.id, b.transactionId)
          .and
          .eq(t.buyerId, buyerId.id)
          .toSQLSyntax
      )
    }

    val userSeller = filter.userSeller.map { sellerId =>
      val t = transactionMySqlRepository.syntax("t")
      sqls.exists(
        select(sqls"1")
          .from(transactionMySqlRepository as t)
          .where
          .eq(t.id, b.transactionId)
          .and
          .eq(t.sellerId, sellerId.id)
          .toSQLSyntax
      )
    }

    val filters = List(
      transactionId,
      offeredResourceId,
      rejected,
      userBuyer,
      userSeller
    ).flatten

    if (filters.nonEmpty) Some(sqls.joinWithAnd(filters: _*)) else None
  }

  private def buildOrderBy(pag: Pagination): Seq[SQLSyntax] = {
    val defaultOrderBy = Seq(b.transactionId, b.barterId)

    pag.orderBy match {
      case Nil => defaultOrderBy
      case seq =>
        val result = seq.flatMap {
          case "transactionId"     => Some(b.transactionId)
          case "offeredResourceId" => Some(b.offeredResourceId)
          case "rejectedAt"        => Some(b.rejectedAt)
          case _                   => None
        }

        if (result.isEmpty) defaultOrderBy else result
    }
  }

}
