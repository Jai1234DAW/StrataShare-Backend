package dev.pompilius.badge.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.badge.domain.Badge
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import jakarta.inject.Singleton
import play.api.libs.json._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BadgeWriterImpl])
trait BadgeWriter {
  def toJson(badge: Badge): Future[JsValue]
  def asList(badges: List[Badge]): Future[JsValue]
  def asAdmin(badge: Badge): Future[JsValue]
}

@Singleton
class BadgeWriterImpl @Inject()(implicit val ec: ExecutionContext) extends BadgeWriter {

  override def toJson(badge: Badge): Future[JsValue] =
    Future.successful(
      Json.obj(
        List(
          toJsValueWrapper(Strings.id, badge.id.toString),
          toJsValueWrapper(Strings.badgeType, badge.badgeType.toString),
          toJsValueWrapper(Strings.name, badge.name),
          toJsValueWrapper(Strings.description, badge.description),
          toJsValueWrapper(Strings.imageUrl, badge.imageUrl)
        ).flatten: _*
      )
    )

  def asList(badges: List[Badge]): Future[JsValue] =
    Future.sequence(badges.map(toJson)).map(jsons => Json.toJson(jsons))

  override def asAdmin(badge: Badge): Future[JsValue] =
    toJson(badge).map { baseJson =>
      baseJson.as[JsObject] ++ Json.obj(
        List(
          toJsValueWrapper(Strings.created, badge.created)
        ).flatten: _*
      )
    }
}