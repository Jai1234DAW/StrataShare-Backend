package dev.pompilius.resource.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.resource.domain.{Resource, ResourceAccessLevel}
import dev.pompilius.sample.domain.Sample
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.study.domain.Study
import dev.pompilius.users.domain.UserId
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ResourceWriterImpl])
trait ResourceWriter {
  def toJson(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue]
  def asPrivate(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue]
  def asPublic(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue]
}

@Singleton
class ResourceWriterImpl @Inject() ()(implicit val ec: ExecutionContext) extends ResourceWriter {

  private def buildBaseResourceJson(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId:UserId,
  ): JsObject = {
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
        toJsValueWrapper(Strings.isBarter, resource.isBarter),
        toJsValueWrapper(Strings.accessLevel, accessLevel.toString),
        toJsValueWrapper(Strings.ownerId, ownerId.toString)
      ).flatten: _*
    )
  }

  private def withSpecificData(
      baseJson: JsObject,
      sample: Option[Sample],
      study: Option[Study],
      fullAccess: Boolean
  ): JsValue = {
    val specificData =
      if (fullAccess) buildSpecificDataFull(sample, study)
      else buildSpecificDataPreview(sample, study)

    val fieldNameOpt = (sample, study) match {
      case (Some(_), None) => Some(Strings.sampleData)
      case (None, Some(_)) => Some(Strings.studyData)
      case (None, None)    => None
      case _ =>
        throw new IllegalArgumentException(
          "Invalid resource: cannot have both sample and study data"
        )
    }

    fieldNameOpt match {
      case Some(fieldName) =>
        specificData match {
          case Some(data) => baseJson ++ Json.obj(fieldName -> data)
          case None       => baseJson
        }

      case None =>
        baseJson
    }
  }

  override def toJson(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource, accessLevel,ownerId)
      withSpecificData(baseJson, sample, study, fullAccess = false)
    }
  }

  override def asPrivate(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource, accessLevel, ownerId)
      withSpecificData(baseJson, sample, study, fullAccess = false)
    }
  }

  override def asPublic(
      resource: Resource,
      accessLevel: ResourceAccessLevel,
      ownerId: UserId,
      sample: Option[Sample] = None,
      study: Option[Study] = None
  ): Future[JsValue] = {
    Future.successful {
      val baseJson = buildBaseResourceJson(resource, accessLevel, ownerId)
      withSpecificData(baseJson, sample, study, fullAccess = true)
    }
  }

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

  private def buildSampleJsonPreview(sample: Sample): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
        toJsValueWrapper(Strings.isFresh, sample.isFresh),
        toJsValueWrapper(Strings.sampleType, sample.sampleType),
        toJsValueWrapper(Strings.sampleCategory, sample.sampleCategory)
      ).flatten: _*
    )
  }

  private def buildSampleJsonFull(sample: Sample): JsObject = {
    Json.obj(
      List(
        toJsValueWrapper(Strings.id, sample.id.toString),
        toJsValueWrapper(Strings.isFresh, sample.isFresh),
        toJsValueWrapper(Strings.minerals, sample.minerals),
        toJsValueWrapper(Strings.collectionMethods, sample.collectionMethods),
        toJsValueWrapper(Strings.sampleType, sample.sampleType),
        toJsValueWrapper(Strings.materialsUsed, sample.materialsUsed),
        toJsValueWrapper(Strings.sampleCategory, sample.sampleCategory),
        toJsValueWrapper(Strings.geologicalProcesses, sample.geologicalProcesses)
      ).flatten: _*
    )
  }

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
