package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.{Resource, ResourceType}
import dev.pompilius.sample.domain.Sample
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.study.domain.Study
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ResourceWriterImpl])
trait ResourceWriter {
  def toJson(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
  def asPrivate(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
  def asPublic(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
}

@Singleton
class ResourceWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends ResourceWriter {

  // Método privado: Construir JSON base del recurso (sin datos específicos)
  private def buildBaseResourceJson(resource: Resource): JsObject = {
      Json.obj(
        List(
          toJsValueWrapper(Strings.resourceId, resource.id.toString),
          toJsValueWrapper(Strings.resourceType, resource.resourceType.toString),
          toJsValueWrapper(Strings.name, resource.name),
          toJsValueWrapper(Strings.visibility, resource.visibility.toString),
          toJsValueWrapper(Strings.location, resource.location),
          toJsValueWrapper(Strings.observations, resource.observations),
          toJsValueWrapper(Strings.summary, resource.summary),
          toJsValueWrapper(Strings.created, resource.created),
          toJsValueWrapper(Strings.updated, resource.updated),
          toJsValueWrapper(Strings.price, resource.price),
          toJsValueWrapper(Strings.isBarter, resource.isBarter)
        ).flatten: _*
      )
    }

  // Método privado: Agregar datos específicos (sample o study) al JSON base
  private def withSpecificData(
      baseJson: JsValue,
      sample: Option[Sample],
      study: Option[Study],
      fullAccess: Boolean
  ): JsValue = {
    val specificData = if (fullAccess) {
      buildSpecificDataFull(sample, study)
    } else {
      buildSpecificDataPreview(sample, study)
    }

    val fieldName = (sample, study) match {
      case (Some(_), None) => Strings.sampleData
      case (None, Some(_)) => Strings.studyData
      case _               => return baseJson
    }

    specificData match {
      case Some(data) => baseJson.as[JsObject] ++ Json.obj(fieldName -> data)
      case None       => baseJson
    }
  }

  // BASE: Preview/metadata básica - Lo que TODOS ven sin acceso
  override def toJson(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource)
      withSpecificData(baseJson, sample, study, fullAccess = false)
    }
  }

  // PRIVATE: Solo preview/teaser para recursos privados sin acceso
  override def asPrivate(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource)
      withSpecificData(baseJson, sample, study, fullAccess = false)
    }
  }

  // PUBLIC: Datos completos (para todos con acceso completo: owner, comprador, o público)
  override def asPublic(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource)
      withSpecificData(baseJson, sample, study, fullAccess = true)
    }
  }

  // Datos específicos en modo PREVIEW (solo lo básico)
  private def buildSpecificDataPreview(
      sample: Option[Sample],
      study: Option[Study]
  ): Option[JsValue] = {
    (sample, study) match {
      case (Some(s), None)  => Some(buildSampleJsonPreview(s))
      case (None, Some(st)) => Some(buildStudyJsonPreview(st))
      case (None, None)     => None
      case _ =>
        throw new IllegalArgumentException(
          "Invalid resource: cannot have both sample and study data"
        )
    }
  }

  // Datos específicos COMPLETOS
  private def buildSpecificDataFull(
      sample: Option[Sample],
      study: Option[Study]
  ): Option[JsValue] = {
    (sample, study) match {
      case (Some(s), None)  => Some(buildSampleJsonFull(s))
      case (None, Some(st)) => Some(buildStudyJsonFull(st))
      case (None, None)     => None
      case _ =>
        throw new IllegalArgumentException(
          "Invalid resource: cannot have both sample and study data"
        )
    }
  }

  // Sample: PREVIEW (lo básico para usuarios sin acceso)
  private def buildSampleJsonPreview(sample: Sample): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
        toJsValueWrapper(Strings.isFresh, sample.isFresh),
        toJsValueWrapper(Strings.sampleType, sample.sampleType),
        toJsValueWrapper(Strings.rockType, sample.rockType)
      ).flatten: _*
    )
  }

  // Sample: COMPLETO (para owner/comprador/público en caso de estarlo)
  private def buildSampleJsonFull(sample: Sample): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
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

  // Study: PREVIEW (lo básico para usuarios sin acceso)
  private def buildStudyJsonPreview(study: Study): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, study.id.toString),
        toJsValueWrapper(Strings.startDate, study.startDate),
        toJsValueWrapper(Strings.endDate, study.endDate),
        toJsValueWrapper(Strings.area, study.area.toString),
        toJsValueWrapper(Strings.authors, study.authors)
      ).flatten: _*
    )
  }

  // Study: COMPLETO (para owner/comprador/público)
  private def buildStudyJsonFull(study: Study): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, study.id.toString),
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
