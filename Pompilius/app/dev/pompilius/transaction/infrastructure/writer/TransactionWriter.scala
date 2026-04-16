package dev.pompilius.transaction.infrastructure.writer

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.transaction.domain.Transaction
import jakarta.inject.Singleton
import play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[TransactionWriterImpl])
trait TransactionWriter {
  def toJson(transaction: Transaction): Future[JsValue]
  def asList(transactions: List[Transaction]): Future[JsValue]
  //def asAdmin(transaction: Transaction): Future[JsValue]
}

@Singleton
class TransactionWriterImpl @Inject()(implicit val ec: ExecutionContext) extends TransactionWriter {

  override def toJson(transaction: Transaction): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.toJson(
          Json.obj(
            List(
              toJsValueWrapper(Strings.transactionType, transaction.transactionType.toString),
              toJsValueWrapper(Strings.transactionStatus, transaction.transactionStatus.toString),
              toJsValueWrapper(Strings.sellerId, transaction.sellerId.toString),
              toJsValueWrapper(Strings.buyerId, transaction.buyerId.toString),
              toJsValueWrapper(Strings.resourceId, transaction.resourceId.toString),
              toJsValueWrapper(Strings.created, transaction.created),
              toJsValueWrapper(Strings.updated, transaction.updated),
              toJsValueWrapper(Strings.metadata, transaction.metadata),
              toJsValueWrapper(Strings.completedAt, transaction.completedAt),
              toJsValueWrapper(Strings.cancelledAt, transaction.cancelledAt)
            ).flatten: _*
          )
        )
      )
    }
  }

  override def asList(transactions: List[Transaction]): Future[JsValue] = {
      Future.sequence(transactions.map(toJson)).map { transactionsToJson =>
        JsArray(transactionsToJson)
      }
    }

}
