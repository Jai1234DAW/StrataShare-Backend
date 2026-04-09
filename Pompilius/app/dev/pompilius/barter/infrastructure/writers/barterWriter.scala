package dev.pompilius.barter.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.barter.domain.Barter
import dev.pompilius.resource.domain.Resource
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import dev.pompilius.transaction.domain.Transaction
import play.api.libs.json.{JsValue, Json}

import javax.inject.Singleton
import scala.concurrent.Future

@ImplementedBy(classOf[BarterWriterImpl])
trait BarterWriter {
  def asJson(barter: Barter, transaction: Transaction, requestedResource: Resource): Future[JsValue]
  def asJsonComplete(
      barter: Barter,
      transaction: Transaction,
      requestedResource: Resource,
      offeredResource: Resource
  ): Future[JsValue]
}

@Singleton
class BarterWriterImpl extends BarterWriter {

  override def asJson(barter: Barter, transaction: Transaction, requestedResource: Resource): Future[JsValue] = {
    Future.successful(
      Json.obj(
        Strings.barterId -> barter.barterId.toString,
        Strings.transactionId -> barter.transactionId.toString,
        Strings.transactionStatus -> transaction.transactionStatus.toString,
        Strings.offeredResourceId -> barter.offeredResourceId.toString,
        Strings.requestedResourceId -> transaction.resourceId.toString,
        Strings.rejectedAt -> barter.rejectedAt.map(_.toString),
        Strings.created -> transaction.created.toString,
        Strings.updated -> transaction.updated.toString
      )
    )
  }

  //MIRAR ESTO OJO

  override def asJsonComplete(
      barter: Barter,
      transaction: Transaction,
      requestedResource: Resource,
      offeredResource: Resource
  ): Future[JsValue] = {
    Future.successful(
      Json.obj(
        Strings.barterId -> barter.barterId.toString,
        Strings.transactionId -> barter.transactionId.toString,
        Strings.transactionStatus -> transaction.transactionStatus.toString,
        Strings.transactionType -> transaction.transactionType.toString,
        Strings.sellerId -> transaction.sellerId.toString,
        Strings.buyerId -> transaction.buyerId.toString,
        Strings.requestedResourceId-> transaction.resourceId.toString,
        Strings.offeredResource -> Json.obj(
          Strings.id -> offeredResource.id.toString,
          Strings.resourceType -> offeredResource.resourceType.toString,
          Strings.visibility -> offeredResource.visibility.toString,
          Strings.localization -> offeredResource.localization
        ),
        Strings.rejectedAt -> barter.rejectedAt.map(_.toString),
        Strings.completedAt -> transaction.completedAt.map(_.toString),
        Strings.cancelledAt -> transaction.cancelledAt.map(_.toString),
        Strings.created -> transaction.created.toString,
        Strings.updated -> transaction.updated.toString
      )
    )
  }
}
