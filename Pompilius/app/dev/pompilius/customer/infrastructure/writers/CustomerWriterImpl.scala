package dev.pompilius.customer.infrastructure.writers

import dev.pompilius.Strings
import dev.pompilius.customer.domain.{Customer, CustomerWriter}
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class CustomerWriterImpl @Inject() extends CustomerWriter {

  override def toJson(customer: Customer): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.userId, customer.userId.toString),
            toJsValueWrapper(Strings.gateway, customer.gateway),
            toJsValueWrapper(Strings.gatewayCustomerId, customer.gatewayCustomerId)
          ).flatten: _*
        )
      )
    }
  }

}
