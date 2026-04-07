package dev.pompilius.study.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import dev.pompilius.study.domain.StudySample
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[StudySampleWriterImpl])
trait StudySampleWriter {
  def asJson(studySample: StudySample): Future[JsValue]
}

@Singleton
class StudySampleWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends StudySampleWriter {

  override def asJson(studySample: StudySample): Future[JsValue] = {
    Future.successful(
      Json.obj(
        List(
          toJsValueWrapper(Strings.studyId, studySample.studyId.toString),
          toJsValueWrapper(Strings.sampleId, studySample.sampleId.toString)
        ).flatten: _*
      )
    )
  }
}
