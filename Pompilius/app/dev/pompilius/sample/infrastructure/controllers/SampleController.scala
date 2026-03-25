package dev.pompilius.sample.infrastructure.controllers

import dev.pompilius.shared.domain.Pagination
import dev.pompilius.sample.domain.exceptions.SampleNotFoundException
import dev.pompilius.sample.domain.{Sample, SampleFilter, SampleId, SampleRepository}
import dev.pompilius.sample.infrastructure.parsers.{CreateSampleRequestParser, UpdateSampleRequestParser}
import dev.pompilius.sample.infrastructure.writers.SampleWriter
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SampleController @Inject() (
    sampleRepository: SampleRepository
)(implicit val ec: ExecutionContext)
    extends BaseController {

  private val logger = play.api.Logger(getClass)

  def createSample: Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val createRequest = CreateSampleRequestParser.parse(request)

        val newSample = createRequest.copy(
          id = SampleId.gen(1)
        )

        sampleRepository.save(newSample).map { _ =>
          Ok(Json.obj(
            "status" -> "success",
            "message" -> "Sample created successfully",
            "data" -> SampleWriter.toJson(newSample)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error creating sample", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def getSampleById(sampleId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = SampleId(sampleId)
        sampleRepository.findById(sId).map {
          case Some(sample) =>
            Ok(Json.obj(
              "status" -> "success",
              "data" -> SampleWriter.toJson(sample)
            ))
          case None =>
            NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Sample with id $sampleId not found"
            ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error fetching sample", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def searchSamples(
      nameOpt: Option[String],
      sampleTypeOpt: Option[String],
      rockTypeOpt: Option[String],
      isFreshOpt: Option[String],
      page: Int = 0,
      limit: Int = 10
  ): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val isFresh = isFreshOpt.map(_.toBoolean)
        val filter = SampleFilter(
          name = nameOpt,
          sampleType = sampleTypeOpt,
          rockType = rockTypeOpt,
          isFresh = isFresh
        )

        val pagination = Pagination(offset = page * limit, limit = limit)

        sampleRepository.find(filter, pagination).map { samples =>
          Ok(Json.obj(
            "status" -> "success",
            "count" -> samples.length,
            "data" -> SampleWriter.toJsonList(samples)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error searching samples", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def searchSamplesByName(name: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        sampleRepository.findByName(name).map { samples =>
          Ok(Json.obj(
            "status" -> "success",
            "count" -> samples.length,
            "data" -> SampleWriter.toJsonList(samples)
          ))
        }
      } catch {
        case e: Exception =>
          logger.error("Error searching samples by name", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def updateSample(sampleId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = SampleId(sampleId)
        val updateRequest = UpdateSampleRequestParser.parse(request)

        sampleRepository.findById(sId).flatMap {
          case Some(existingSample) =>
            val updatedSample = existingSample.copy(
              name = updateRequest.name.getOrElse(existingSample.name),
              description = updateRequest.description.orElse(existingSample.description),
              minerals = updateRequest.minerals.getOrElse(existingSample.minerals),
              collectionMethods = updateRequest.collectionMethods.getOrElse(existingSample.collectionMethods),
              isFresh = updateRequest.isFresh.getOrElse(existingSample.isFresh),
              sampleType = updateRequest.sampleType.getOrElse(existingSample.sampleType),
              materialsUsed = updateRequest.materialsUsed.getOrElse(existingSample.materialsUsed),
              rockType = updateRequest.rockType.getOrElse(existingSample.rockType),
              geologicalProcesses = updateRequest.geologicalProcesses.getOrElse(existingSample.geologicalProcesses),
              updated = DateTime.now()
            )

            sampleRepository.save(updatedSample).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Sample updated successfully",
                "data" -> SampleWriter.toJson(updatedSample)
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Sample with id $sampleId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error updating sample", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }

  def deleteSample(sampleId: String): Action[AnyContent] = {
    Action.async { implicit request =>
      try {
        val sId = SampleId(sampleId)

        sampleRepository.findById(sId).flatMap {
          case Some(_) =>
            sampleRepository.delete(sId).map { _ =>
              Ok(Json.obj(
                "status" -> "success",
                "message" -> "Sample deleted successfully"
              ))
            }
          case None =>
            Future(NotFound(Json.obj(
              "status" -> "error",
              "message" -> s"Sample with id $sampleId not found"
            )))
        }
      } catch {
        case e: Exception =>
          logger.error("Error deleting sample", e)
          Future(BadRequest(Json.obj(
            "status" -> "error",
            "message" -> e.getMessage
          )))
      }
    }
  }
}

