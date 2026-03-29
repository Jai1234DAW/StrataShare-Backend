package dev.pompilius.resource.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.resource.domain.ResourceType
import dev.pompilius.resource.domain.request.{CreateResourceRequest, CreateSampleResourceRequest, CreateStudyResourceRequest, SampleData, StudyData}
import dev.pompilius.resource.domain.study.Area
import dev.pompilius.shared.domain.Visibility
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.{ReadsUtil, StringUtil}
import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}

object CreateResourceRequestParser {

  // Parser para SampleData
  implicit val sampleDataReads: Reads[SampleData] = (
    (__ \ Strings.name).read[String](ReadsUtil.sampleName) and
      (__ \ Strings.isFresh).read[Boolean] and
      (__ \ Strings.minerals).readNullable[String](ReadsUtil.minerals) and
      (__ \ Strings.collectionMethods).readNullable[String](ReadsUtil.collectionMethods) and
      (__ \ Strings.sampleType).readNullable[String](ReadsUtil.sampleType) and
      (__ \ Strings.materialsUsed).readNullable[String](ReadsUtil.materialsUsed) and
      (__ \ Strings.rockType).readNullable[String](ReadsUtil.rockType) and
      (__ \ Strings.geologicalProcesses).readNullable[String](ReadsUtil.geologicalProcesses)
  )(SampleData.apply _)

  // Parser para StudyData
  implicit val studyDataReads: Reads[StudyData] = (
    (__ \ Strings.name).read[String](ReadsUtil.studyName) and
      (__ \ Strings.startDate).read[String].map(s => DateTime.parse(s)) and
      (__ \ Strings.description).read[String](ReadsUtil.studyDescription).map(StringUtil.stripTags) and
      (__ \ Strings.coordinates).read[String](ReadsUtil.coordinates) and
      (__ \ Strings.area).read[String].map(Area.withNameInsensitive) and
      (__ \ Strings.methods).read[String](ReadsUtil.methods) and
      (__ \ Strings.authors).read[String](ReadsUtil.authors) and
      (__ \ Strings.section).read[Boolean] and
      (__ \ Strings.endDate).readNullable[String].map(_.map(s => DateTime.parse(s))) and
      (__ \ Strings.antecedents).readNullable[Boolean] and
      (__ \ Strings.nameSection).readNullable[String](ReadsUtil.nameSection)
  )(StudyData.apply _)

  // Parser para CreateResourceRequest
  implicit val jsonReads: Reads[CreateResourceRequest] = new Reads[CreateResourceRequest] {
    def reads(json: JsValue): JsResult[CreateResourceRequest] = {
      val resourceType = ResourceType.withNameInsensitive((json \ Strings.resourceType).as[String])
      val visibility = Visibility.withNameInsensitive((json \ Strings.visibility).as[String])
      val localization = (json \ Strings.localization).asOpt[String]
      val observations = (json \ Strings.observations).asOpt[String].map(StringUtil.stripTags)
      val summary = (json \ Strings.summary).asOpt[String].map(StringUtil.stripTags)

      // Parsear los datos específicos usando los Reads implícitos
      val sampleData = (json \ Strings.sampleData).asOpt[SampleData]
      val studyData = (json \ Strings.studyData).asOpt[StudyData]

      // Crear la instancia correcta del sealed trait según resourceType
      (resourceType, sampleData, studyData) match {
        // SAMPLE: obligatorio sampleData, NO studyData
        case (ResourceType.SAMPLE, Some(sample), None) =>
          JsSuccess(CreateSampleResourceRequest(
            visibility = visibility,
            localization = localization,
            observations = observations,
            summary = summary,
            sampleData = sample
          ))
        case (ResourceType.SAMPLE, None, _) =>
          JsError("sampleData is required when resourceType is SAMPLE")
        case (ResourceType.SAMPLE, _, Some(_)) =>
          JsError("Cannot send both sampleData and studyData simultaneously")

        // STUDY: obligatorio studyData, NO sampleData
        case (ResourceType.STUDY, None, Some(study)) =>
          JsSuccess(CreateStudyResourceRequest(
            visibility = visibility,
            localization = localization,
            observations = observations,
            summary = summary,
            studyData = study
          ))
        case (ResourceType.STUDY, _, None) =>
          JsError("studyData is required when resourceType is STUDY")
        case (ResourceType.STUDY, Some(_), _) =>
          JsError("Cannot send both sampleData and studyData simultaneously")
      }
    }
  }

  def parse[A](request: Request[A]): CreateResourceRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        json.as[CreateResourceRequest]
      case _ =>
        throw new BadRequestException("Expecting text/json or application/json body")
    }
  }
}
