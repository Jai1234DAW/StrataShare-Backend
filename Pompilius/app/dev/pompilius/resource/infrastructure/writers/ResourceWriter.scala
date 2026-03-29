package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.sample.Sample
import dev.pompilius.resource.domain.study.Study
import dev.pompilius.resource.domain.{Resource, ResourceType}
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.users.infrastructure.writers.UserWriter
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
class ResourceWriterImpl @Inject() (
    userWriter: UserWriter
)(implicit val ec: ExecutionContext)
    extends ResourceWriter {

  override def toJson(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsObject] = {
    Future.successful {
      // Construir JSON específico (Sample o Study)
      val specificData = buildSpecificData(sample, study)

      val finalJson = Json.obj(
        List(
          toJsValueWrapper(Strings.id, resource.id.toString),
          toJsValueWrapper(Strings.resourceType, resource.resourceType.toString),
          toJsValueWrapper(Strings.visibility, resource.visibility.toString),
          toJsValueWrapper(Strings.localization, resource.localization),
          toJsValueWrapper(Strings.observations, resource.observations),
          toJsValueWrapper(Strings.summary, resource.summary),
          toJsValueWrapper(Strings.created, resource.created),
          toJsValueWrapper(Strings.updated, resource.updated),
          toJsValueWrapper(
            resource.resourceType match {
              case ResourceType.SAMPLE => Strings.sampleData
              case ResourceType.STUDY => Strings.studyData
            },
            specificData
          )
        ).flatten: _*
      )

      finalJson
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
      baseJson ++ Json.obj(
        List(
          toJsValueWrapper(Strings.deleted, resource.deleted)
        ).flatten: _*
      )
    }
  }

  override def asPublic(
      resource: Resource,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    for {
      baseJson <- toJson(resource, sample, study)
    } yield {
      baseJson
    }
  }

  // ===== MÉTODOS PRIVADOS =====

  private def buildSpecificData(sample: Option[Sample], study: Option[Study]): Option[JsObject] = {
    (sample, study) match {
      case (Some(s), None) => Some(buildSampleJson(s))
      case (None, Some(st)) => Some(buildStudyJson(st))
      case _ => None
    }
  }

  private def buildSampleJson(sample: Sample): JsObject = {
    Json.obj(
      List(
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
