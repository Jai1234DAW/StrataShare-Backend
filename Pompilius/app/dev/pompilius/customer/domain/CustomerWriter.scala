package dev.pompilius.customer.domain

import com.google.inject.ImplementedBy
import dev.pompilius.customer.infrastructure.writers.CustomerWriterImpl
import play.api.libs.json._

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerWriterImpl])
trait CustomerWriter {
  def toJson(customer: Customer): Future[JsValue]
}
