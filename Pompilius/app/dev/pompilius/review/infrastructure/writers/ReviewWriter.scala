package dev.pompilius.review.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.review.domain.Review
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import play.api.libs.json.{JsObject, JsValue, Json}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
@ImplementedBy(classOf[ReviewWriterImpl])
trait ReviewWriter {
  def asJson(review: Review): Future[JsValue]
  def asJsonWithUsername(review: Review, username: String): Future[JsValue]
}
@Singleton
class ReviewWriterImpl @Inject() ()(implicit ec: ExecutionContext) extends ReviewWriter {
  override def asJson(review: Review): Future[JsValue] = {
    Future.successful(
      Json.obj(
        List(
          toJsValueWrapper(Strings.id -> review.id.toString),
          toJsValueWrapper(Strings.resourceId -> review.resourceId.toString),
          toJsValueWrapper(Strings.userId -> review.userId.toString),
          toJsValueWrapper(Strings.rating -> review.rating),
          toJsValueWrapper(Strings.comment -> review.comment),
          toJsValueWrapper(Strings.created -> review.createdAt.toString),
          toJsValueWrapper(Strings.updated -> review.updatedAt.toString)
        ).flatten: _*
      )
    )
  }
  override def asJsonWithUsername(review: Review, username: String): Future[JsValue] = {
    for {
      baseJson <- asJson(review).map(_.as[JsObject])
    } yield {
      baseJson ++ Json.obj(
        List(
          toJsValueWrapper(Strings.username -> username)
        ).flatten: _*
      )
    }
  }
}
