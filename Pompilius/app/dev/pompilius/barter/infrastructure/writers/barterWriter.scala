package dev.pompilius.barter.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.barter.domain.Barter
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import dev.pompilius.transaction.domain.Transaction
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarterWriterImpl])
trait BarterWriter {
  def asJson(barter: Barter, transaction: Transaction): Future[JsValue]
  def asJsonComplete(
      barter: Barter,
      transaction: Transaction
  ): Future[JsValue]
}

@Singleton
class BarterWriterImpl @Inject() (implicit ec: ExecutionContext) extends BarterWriter {

  override def asJson(barter: Barter, transaction: Transaction): Future[JsObject] = {
    Future.successful {
      Json.obj(
        List(
          toJsValueWrapper(Strings.barterId -> barter.barterId.toString),
          toJsValueWrapper(Strings.transactionId -> barter.transactionId.toString),
          toJsValueWrapper(Strings.transactionType -> transaction.transactionType.toString),
          toJsValueWrapper(Strings.transactionStatus -> transaction.transactionStatus.toString),
          toJsValueWrapper(Strings.offeredResourceId -> barter.offeredResourceId.toString),
          toJsValueWrapper(Strings.requestedResourceId -> transaction.resourceId.toString),
          toJsValueWrapper(Strings.created -> transaction.created.toString)
        ).flatten: _*
      )

    }
  }
  //MIRAR ESTO OJO

  override def asJsonComplete(
      barter: Barter,
      transaction: Transaction
  ): Future[JsValue] = {
    for {
      baseJson <- asJson(barter, transaction)
    } yield {
      baseJson ++ Json.obj(
        List(
          toJsValueWrapper(Strings.sellerId -> transaction.sellerId.toString),
          toJsValueWrapper(Strings.buyerId -> transaction.buyerId.toString),
          toJsValueWrapper(Strings.updated -> transaction.updated.toString),
          toJsValueWrapper(Strings.completedAt -> transaction.completedAt.map(_.toString).getOrElse(""))
        ).flatten: _*
      )
    }
  }
}
