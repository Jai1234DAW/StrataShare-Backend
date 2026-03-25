package dev.pompilius.study.infrastructure.writers

import dev.pompilius.study.domain.{Study, StudyWithAttachments}
import play.api.libs.json._

object StudyWithAttachmentsWriter {

  implicit val studyWithAttachmentsWrites: Writes[StudyWithAttachments] = new Writes[StudyWithAttachments] {
    override def writes(studyWithAttachments: StudyWithAttachments): JsValue = {
      val study = studyWithAttachments.study
      Json.obj(
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
        "attachments" -> studyWithAttachments.attachments.map(_.toString),
        "samples" -> study.samples
      )
    }
  }

  def toJson(studyWithAttachments: StudyWithAttachments): JsValue = Json.toJson(studyWithAttachments)

  def toJsonList(list: List[StudyWithAttachments]): JsValue = Json.toJson(list)
}

