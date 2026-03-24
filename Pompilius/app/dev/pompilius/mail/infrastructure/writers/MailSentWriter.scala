package dev.pompilius.mail.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.mail.domain.MailSent
import dev.pompilius.shared.infrastructure.JsUtils.{toJsValueWrapper, JodaDateTimeFormat}
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[MailSentWriterImpl])
trait MailSentWriter {
  def asUser(mailSent: MailSent): Future[JsValue]
}

@Singleton
class MailSentWriterImpl @Inject() extends MailSentWriter {

  override def asUser(mailSent: MailSent): Future[JsValue] = {
    Future.successful {
      Json.obj(
        List(
          toJsValueWrapper(Strings.id, mailSent.id.toString),
          toJsValueWrapper(Strings.mailType, mailSent.mailType.toString),
          toJsValueWrapper(Strings.address, mailSent.address),
          toJsValueWrapper(Strings.sentAt, mailSent.sentAt),
          toJsValueWrapper(Strings.userId, mailSent.userId.id.toString)
        ).flatten: _*
      )
    }
  }
}
