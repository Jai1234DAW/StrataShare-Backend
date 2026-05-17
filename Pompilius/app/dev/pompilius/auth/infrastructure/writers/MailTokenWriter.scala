package dev.pompilius.auth.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._
import dev.pompilius.auth.domain.MailToken

import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MailTokenWriterImpl])
trait MailTokenWriter {
  def toString(mailToken: MailToken, secretKey: SecretKeySpec): Future[String]
}

@Singleton
class MailTokenWriterImpl @Inject()(implicit ec: ExecutionContext) extends MailTokenWriter {

  def toJson(mailToken: MailToken): Future[JsValue] = {
    Future.successful {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper("em", mailToken.mail),
            toJsValueWrapper("ex", mailToken.expires.getMillis)
          ).flatten: _*
        )
      )
    }
  }

  override def toString(mailToken: MailToken, secretKey: SecretKeySpec): Future[String] = {
    toJson(mailToken).map{ json =>
      val payload = new Base64(0, Array.empty[Byte], true)
        .encodeAsString(json.toString.getBytes("UTF-8"))
      val signature = mailToken.signature(secretKey)
      s"$payload.$signature"
    }
  }

}
