package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.study.Study
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[StudyWriterImpl])
trait StudyWriter {
  def toJson(study: Study): Future[JsValue]
  def asOwner(study: Study): Future[JsValue]
  def asPublic(study: Study): Future[JsValue]
}

@Singleton
class StudyWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends StudyWriter {

  override def toJson(study: Study): Future[JsValue] = {
    Future.successful {
      buildStudyJson(study)
    }
  }

  override def asOwner(study: Study): Future[JsValue] = {
    for {
      baseJson <- toJson(study)
    } yield {
      // Agregar campos adicionales para el propietario si es necesario
      baseJson
    }
  }

  override def asPublic(study: Study): Future[JsValue] = {
    for {
      baseJson <- toJson(study)
    } yield {
      baseJson
    }
  }

  // ===== MÉTODOS PRIVADOS =====

  private def buildStudyJson(study: Study): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, study.id.toString),
        toJsValueWrapper(Strings.resourceId, study.resourceId.toString),
        toJsValueWrapper(Strings.name, study.name),
        toJsValueWrapper(Strings.startDate, study.startDate),
        toJsValueWrapper(Strings.endDate, study.endDate),
        toJsValueWrapper(Strings.description, study.description),
        toJsValueWrapper(Strings.coordinates, study.coordinates),
        toJsValueWrapper(Strings.area, study.area.toString),
        toJsValueWrapper(Strings.methods, study.methods),
        toJsValueWrapper(Strings.authors, study.authors),
        toJsValueWrapper(Strings.section, study.section),
        toJsValueWrapper(Strings.antecedents, study.antecedents),
        toJsValueWrapper(Strings.nameSection, study.nameSection)
      ).flatten: _*
    )
  }
}

