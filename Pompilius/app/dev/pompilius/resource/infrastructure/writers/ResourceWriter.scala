package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.sample.Sample
import dev.pompilius.resource.domain.study.Study
import dev.pompilius.resource.domain.{Resource, ResourceType}
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ResourceWriterImpl])
trait ResourceWriter {
  def toJson(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
  def asOwner(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
  def asPublic(resource: Resource, sample: Option[Sample] = None, study: Option[Study] = None): Future[JsValue]
}

@Singleton
class ResourceWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends ResourceWriter {

  override def toJson(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = Json.obj(
        List(
          toJsValueWrapper(Strings.resourceId, resource.id.toString),
          toJsValueWrapper(Strings.resourceType, resource.resourceType.toString),
          toJsValueWrapper(Strings.visibility, resource.visibility.toString),
          toJsValueWrapper(Strings.localization, resource.localization),
          toJsValueWrapper(Strings.observations, resource.observations),
          toJsValueWrapper(Strings.summary, resource.summary),
          toJsValueWrapper(Strings.created, resource.created),
          toJsValueWrapper(Strings.updated, resource.updated)
        ).flatten: _*
      )

      val specificData = buildSpecificData(sample, study)
      val fieldName = resource.resourceType match {
        case ResourceType.SAMPLE => Strings.sampleData
        case ResourceType.STUDY  => Strings.studyData
      }

      specificData match {
        case Some(data) => baseJson ++ Json.obj(fieldName -> data)
        case None       => baseJson
      }
    }
  }

  override def asOwner(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    for {
      baseJson <- toJson(resource, sample, study)
    } yield {
      baseJson match {
        case obj: JsObject =>
          obj ++ Json.obj(
            List(
              toJsValueWrapper(Strings.deleted, resource.deleted)
            ).flatten: _*
          )
        case other => other
      }
    }
  }

  override def asPublic(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    toJson(resource, sample, study)
  }

  // ===== MÉTODOS PRIVADOS =====

  private def buildSpecificData(
      sample: Option[Sample],
      study: Option[Study]
  ): Option[JsValue] = {
    (sample, study) match {
      case (Some(s), None)  => Some(buildSampleJson(s))
      case (None, Some(st)) => Some(buildStudyJson(st))
      case _                => None
    }
  }

  private def buildSampleJson(sample: Sample) = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
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

  private def buildStudyJson(study: Study): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, study.id.toString),
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
