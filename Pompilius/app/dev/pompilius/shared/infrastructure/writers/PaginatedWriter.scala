package dev.pompilius.shared.infrastructure.writers

import com.google.inject.ImplementedBy
import play.api.libs.json._
import dev.pompilius.shared.domain.Paginated

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[PaginatedWriterImpl])
trait PaginatedWriter {
  def toJson[T](p: Paginated[T])(implicit f: T => Future[JsValue]): Future[JsValue]
  def toFlattedJson[T](p: Paginated[T])(implicit f: T => Future[Option[JsValue]]): Future[JsValue]
}

@Singleton
class PaginatedWriterImpl @Inject() (implicit val ec: ExecutionContext) extends PaginatedWriter {

  implicit val jsonWrites: Format[Paginated[JsValue]] = Json.format[Paginated[JsValue]]

  override def toJson[T](p: Paginated[T])(implicit f: T => Future[JsValue]): Future[JsValue] = {
    Future.sequence(p.items.map(item => f(item))).map(items => Json.toJson(Paginated(items = items, p.moreItems)))
  }

  def toFlattedJson[T](p: Paginated[T])(implicit f: T => Future[Option[JsValue]]): Future[JsValue] = {
    Future
      .sequence(p.items.map(item => f(item)))
      .map(items => Json.toJson(Paginated(items = items.flatten, p.moreItems)))
  }
}
