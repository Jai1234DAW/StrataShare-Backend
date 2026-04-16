package dev.pompilius.customer.infrastructure.repositories.mysql

import dev.pompilius.customer.domain.{Customer, CustomerRepository}
import dev.pompilius.gateways.domain.Gateway
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import scalikejdbc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CustomerMySqlRepository @Inject() (implicit dbExecutionContext: DbExecutionContext)
    extends CustomerRepository
    with SQLSyntaxSupport[Customer] {

  override val tableName = "customer"

  def apply(c: SyntaxProvider[Customer])(rs: WrappedResultSet): Customer = apply(c.resultName)(rs)
  def apply(c: ResultName[Customer])(rs: WrappedResultSet): Customer =
    Customer(
      userId = UserId(rs.get[Long](c.userId)),
      gateway = Gateway.withNameInsensitive(rs.get[String](c.gateway)),
      gatewayCustomerId = rs.get(c.gatewayCustomerId),
      metadata = rs.get(c.metadata)
    )

  private val c = this.syntax("c")

  override def findByUserId(userId: UserId, gateway: Gateway): Future[Option[Customer]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as c).where.eq(c.userId, userId.id).and.eq(c.gateway, gateway.toString)
        }.map(apply(c.resultName)(_)).single()
      }
    }

  override def getAllByUserId(userId: UserId): Future[List[Customer]] =
    Future {
      DB.localTx { implicit session =>
        withSQL {
          selectFrom(this as c).where.eq(c.userId, userId.id)
        }.map(apply(c.resultName)(_)).list()
      }
    }

  override def save(customer: Customer): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.userId -> customer.userId.id,
          column.gateway -> customer.gateway.toString,
          column.gatewayCustomerId -> customer.gatewayCustomerId,
          column.metadata -> customer.metadata
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

}
