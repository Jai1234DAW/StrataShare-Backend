package dev.pompilius.shared.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.shared.domain.RequestFingerprint
import play.api.libs.json._
import javax.inject.{Inject, Singleton}

import scala.concurrent.Future

@ImplementedBy(classOf[RequestFingerprintWriterImpl])
trait RequestFingerprintWriter {
  def toJson(fingerprint: RequestFingerprint): Future[JsValue]

}

@Singleton
class RequestFingerprintWriterImpl @Inject() extends RequestFingerprintWriter {
  implicit val jsonWrites: Writes[RequestFingerprint] = Json.format[RequestFingerprint]

  override def toJson(fingerprint: RequestFingerprint): Future[JsValue] = {
    Future.successful(Json.toJson(fingerprint))
  }

}
