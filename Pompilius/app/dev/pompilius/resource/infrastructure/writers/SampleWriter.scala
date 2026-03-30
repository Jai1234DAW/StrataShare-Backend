package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.sample.Sample
import dev.pompilius.shared.infrastructure.JsUtils.toJsValueWrapper
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SampleWriterImpl])
trait SampleWriter {
  def toJson(sample: Sample): Future[JsValue]
  def asOwner(sample: Sample): Future[JsValue]
  def asPublic(sample: Sample): Future[JsValue]
}

@Singleton
class SampleWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends SampleWriter {

  override def toJson(sample: Sample): Future[JsValue] = {
    Future.successful {
      buildSampleJson(sample)
    }
  }

  override def asOwner(sample: Sample): Future[JsValue] = {
    for {
      baseJson <- toJson(sample)
    } yield {
      // Agregar campos adicionales para el propietario si es necesario
      baseJson
    }
  }

  override def asPublic(sample: Sample): Future[JsValue] = {
    for {
      baseJson <- toJson(sample)
    } yield {
      baseJson
    }
  }

  // ===== MÉTODOS PRIVADOS =====

  private def buildSampleJson(sample: Sample): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
        toJsValueWrapper(Strings.resourceId, sample.resourceId.toString),
        toJsValueWrapper(Strings.name, sample.name),
        toJsValueWrapper(Strings.isFresh, sample.isFresh),
        toJsValueWrapper(Strings.minerals, sample.minerals),
        toJsValueWrapper(Strings.collectionMethods, sample.collectionMethods),
        toJsValueWrapper(Strings.sampleType, sample.sampleType),
        toJsValueWrapper(Strings.materialsUsed, sample.materialsUsed),
        toJsValueWrapper(Strings.rockType, sample.rockType),
        toJsValueWrapper(Strings.geologicalProcesses, sample.geologicalProcesses)
      ).flatten: _*
    )
  }
}
