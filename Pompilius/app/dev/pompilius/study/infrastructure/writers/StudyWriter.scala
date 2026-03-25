package dev.pompilius.study.infrastructure.writers

import dev.pompilius.study.domain.Study
import play.api.libs.json._

object StudyWriter {

  implicit val studyWrites: Writes[Study] = new Writes[Study] {
    override def writes(study: Study): JsValue = Json.obj(
      "id" -> study.id.toString,
      "name" -> study.name,
      "visibility" -> study.visibility.value,
      "localization" -> study.localization,
      "startDate" -> study.startDate.getMillis,
      "endDate" -> study.endDate.map(_.getMillis),
      "description" -> study.description,
      "coordinates" -> study.coordinates,
      "observations" -> study.observations,
      "summary" -> study.summary,
      "area" -> study.area.entryName,
      "methods" -> study.methods,
      "authors" -> study.authors,
      "antecedent" -> study.antecedent,
      "section" -> study.section,
      "nameSection" -> study.nameSection,
      "created" -> study.created.getMillis,
      "updated" -> study.updated.getMillis,
      "samples" -> study.samples
    )
  }

  def toJson(study: Study): JsValue = Json.toJson(study)

  def toJsonList(studies: List[Study]): JsValue = Json.toJson(studies)
}



