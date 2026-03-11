package dev.pompilius.country.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[CountryWriterImpl])
trait CountryWriter {
  def toJson(country: Country): Future[JsValue]
}

@Singleton
class CountryWriterImpl @Inject() extends CountryWriter {

  override def toJson(country: Country): Future[JsValue] = {
    Future.successful(
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.code, country.toString),
            toJsValueWrapper(Strings.fullName, country.fullName)
          ).flatten: _*
        )
      )
    )
  }
}
