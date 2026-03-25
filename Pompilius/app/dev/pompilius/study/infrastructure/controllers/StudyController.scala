package dev.pompilius.studies.infrastructure.controllers

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Pagination
import dev.pompilius.studies.domain.exceptions.StudyNotFoundException
import dev.pompilius.studies.domain.{Study, StudyAttachmentRepository, StudyFilter, StudyId, StudyRepository}
import dev.pompilius.studies.infrastructure.parsers.{CreateStudyRequestParser, StudyAttachmentRequestParser, UpdateStudyRequestParser}
import dev.pompilius.studies.infrastructure.writers.StudyWriter
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StudyController @Inject() (
    studyRepository: StudyRepository,
    studyAttachmentRepository: StudyAttachmentRepository
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = play.api.Logger(getClass)

  // ...existing code...

  def createStudy: Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val createRequest = CreateStudyRequestParser.parse(request)

        // Generar ID para el nuevo estudio
        val newStudy = createRequest.copy(
          id = StudyId.gen(1) // Usar nodo 1, ajusta según tu configuración
        )

        studyRepository.save(newStudy).map { _ =>
          Ok(Json.obj(
            "status" -> "success",
            "message" -> "Study created successfully",
            "data" -> StudyWriter.toJson(newStudy)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error creating study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def getStudyById(studyId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)
        studyRepository.findById(sId).map {
          case Some(study) =>
            Ok(Json.obj(
              "status" -> "success",
              "data" -> StudyWriter.toJson(study)
            ))
          case None =>
            NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error fetching study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def searchStudies(
      nameOpt: Option[String],
      visibilityOpt: Option[String],
      localizationOpt: Option[String],
      page: Int = 0,
      limit: Int = 10
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val filter = StudyFilter(
          name = nameOpt,
          visibility = visibilityOpt,
          localization = localizationOpt
        )

        val pagination = Pagination(offset = page * limit, limit = limit)

        studyRepository.find(filter, pagination).map { studies =>
          Ok(Json.obj(
            "status" -> "success",
            "count" -> studies.length,
            "data" -> StudyWriter.toJsonList(studies)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error searching studies", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def searchStudiesByName(name: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        studyRepository.findByName(name).map { studies =>
          Ok(Json.obj(
            "status" -> "success",
            "count" -> studies.length,
            "data" -> StudyWriter.toJsonList(studies)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error searching studies by name", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def updateStudy(studyId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)
        val updateRequest = UpdateStudyRequestParser.parse(request)

        studyRepository.findById(sId).flatMap {
          case Some(existingStudy) =>
            val updatedStudy = existingStudy.copy(
              name = updateRequest.name.getOrElse(existingStudy.name),
              visibility = updateRequest.visibility.getOrElse(existingStudy.visibility),
              localization = updateRequest.localization.getOrElse(existingStudy.localization),
              startDate = updateRequest.startDate.map(new DateTime(_)).getOrElse(existingStudy.startDate),
              endDate = updateRequest.endDate.map(new DateTime(_)).orElse(existingStudy.endDate),
              description = updateRequest.description.orElse(existingStudy.description),
              coordinates = updateRequest.coordinates.orElse(existingStudy.coordinates),
              observations = updateRequest.observations.orElse(existingStudy.observations),
              summary = updateRequest.summary.orElse(existingStudy.summary),
              updated = DateTime.now()
            )

            studyRepository.save(updatedStudy).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Study updated successfully",
                "data" -> StudyWriter.toJson(updatedStudy)
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error updating study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def deleteStudy(studyId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)

        studyRepository.findById(sId).flatMap {
          case Some(_) =>
            studyRepository.delete(sId).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Study deleted successfully"
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error deleting study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def addAttachmentToStudy(studyId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)
        val attachmentRequest = StudyAttachmentRequestParser.parseAddAttachment(request)
        val aId = AttachmentId(attachmentRequest.attachmentId)

        studyRepository.findById(sId).flatMap {
          case Some(_) =>
            studyAttachmentRepository.addAttachment(sId, aId).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Attachment added to study successfully"
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error adding attachment to study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def removeAttachmentFromStudy(studyId: String, attachmentId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)
        val aId = AttachmentId(attachmentId)

        studyRepository.findById(sId).flatMap {
          case Some(_) =>
            studyAttachmentRepository.removeAttachment(sId, aId).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Attachment removed from study successfully"
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error removing attachment from study", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def getStudyAttachments(studyId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = StudyId(studyId)

        studyRepository.findById(sId).flatMap {
          case Some(_) =>
            studyAttachmentRepository.getAttachments(sId).map { attachments =>
              Ok(Json.obj(
                "status" -> "success",
                "count" -> attachments.length,
                "data" -> attachments.map(_.toString)
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Study with id $studyId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error fetching study attachments", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }
}



