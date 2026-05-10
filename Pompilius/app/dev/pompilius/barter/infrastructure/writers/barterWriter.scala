package dev.pompilius.barter.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.barter.domain.Barter
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat,toJsValueWrapper}
import dev.pompilius.transaction.domain.Transaction
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BarterWriterImpl])
trait BarterWriter {
  def toJson(transaction: Transaction, barter: Barter): Future[JsValue]
  def asBuyer(transaction: Transaction, barter: Barter): Future[JsValue]
  def asSeller(transaction: Transaction, barter: Barter): Future[JsValue]
  def asAdmin(transaction: Transaction, barter: Barter): Future[JsValue]
}

@Singleton
class BarterWriterImpl @Inject() (implicit ec: ExecutionContext) extends BarterWriter {

  override def toJson(transaction: Transaction, barter: Barter): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.barterId, barter.barterId.toString),
            toJsValueWrapper(Strings.transactionId, barter.transactionId.toString),
            toJsValueWrapper(Strings.transactionType, transaction.transactionType.toString),
            toJsValueWrapper(Strings.transactionStatus, transaction.transactionStatus.toString),
            toJsValueWrapper(Strings.offeredResourceId, barter.offeredResourceId.toString),
            toJsValueWrapper(Strings.requestedResourceId, transaction.resourceId.toString),
            toJsValueWrapper(Strings.created, transaction.created)
          ).flatten: _*
        )
      )
    }
  }

  override def asBuyer(transaction: Transaction, barter: Barter): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, barter)
    } yield {
      val commonFields = List(
        toJsValueWrapper(Strings.sellerId, transaction.sellerId.toString),
        toJsValueWrapper(Strings.updated, transaction.updated)
      ).flatten

      val completedField =
        transaction.completedSuccessfullyAt
          .map(date => toJsValueWrapper(Strings.completedAt, date))
          .getOrElse(Nil)

      baseJson.as[JsObject] ++ Json.obj((commonFields ++ completedField): _*)
    }
  }

  override def asSeller(transaction: Transaction, barter: Barter): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, barter)
    } yield {
      val commonFields = List(
        toJsValueWrapper(Strings.buyerId, transaction.buyerId.toString),
        toJsValueWrapper(Strings.updated, transaction.updated)
      ).flatten

      val completedField =
        transaction.completedSuccessfullyAt
          .map(date => toJsValueWrapper(Strings.completedAt, date))
          .getOrElse(Nil)

      baseJson.as[JsObject] ++ Json.obj((commonFields ++ completedField): _*)
    }
  }

  override def asAdmin(transaction: Transaction, barter: Barter): Future[JsValue] = {
    for {
      baseJson <- toJson(transaction, barter)
    } yield {
      baseJson.as[JsObject] ++ Json.obj(
        List(
          toJsValueWrapper(Strings.sellerId, transaction.sellerId.toString),
          toJsValueWrapper(Strings.buyerId, transaction.buyerId.toString),
          toJsValueWrapper(Strings.updated, transaction.updated),
          toJsValueWrapper(Strings.completedAt, transaction.completedSuccessfullyAt)
        ).flatten: _*
      )
    }
  }
}