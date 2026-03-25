package dev.pompilius.sample.infrastructure.writers

import dev.pompilius.sample.domain.Sample
import play.api.libs.json._

object SampleWriter {

  implicit val sampleWrites: Writes[Sample] = new Writes[Sample] {
    override def writes(sample: Sample): JsValue = Json.obj(
      "id" -> sample.id.toString,
      "name" -> sample.name,
      "description" -> sample.description,
      "minerals" -> sample.minerals,
      "collectionMethods" -> sample.collectionMethods,
      "isFresh" -> sample.isFresh,
      "sampleType" -> sample.sampleType,
      "materialsUsed" -> sample.materialsUsed,
      "rockType" -> sample.rockType,
      "geologicalProcesses" -> sample.geologicalProcesses,
      "created" -> sample.created.getMillis,
      "updated" -> sample.updated.getMillis
    )
  }

  def toJson(sample: Sample): JsValue = Json.toJson(sample)

  def toJsonList(samples: List[Sample]): JsValue = Json.toJson(samples)
}

