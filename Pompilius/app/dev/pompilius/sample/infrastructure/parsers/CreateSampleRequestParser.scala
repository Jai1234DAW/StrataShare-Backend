package dev.pompilius.sample.infrastructure.parsers

import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.sample.domain.{Sample, SampleId}
import org.joda.time.DateTime
import play.api.mvc.Request

object CreateSampleRequestParser {

  def parse(request: Request[play.api.libs.json.JsValue]): Sample = {
    try {
      val json = request.body

      val name = (json \ "name").as[String]
      val description = (json \ "description").asOpt[String]
      val minerals = (json \ "minerals").as[String]
      val collectionMethods = (json \ "collectionMethods").as[String]
      val isFresh = (json \ "isFresh").as[Boolean]
      val sampleType = (json \ "sampleType").as[String]
      val materialsUsed = (json \ "materialsUsed").as[String]
      val rockType = (json \ "rockType").as[String]
      val geologicalProcesses = (json \ "geologicalProcesses").as[String]

      val now = DateTime.now()

      Sample(
        id = SampleId.gen(1),
        name = name,
        description = description,
        minerals = minerals,
        collectionMethods = collectionMethods,
        isFresh = isFresh,
        sampleType = sampleType,
        materialsUsed = materialsUsed,
        rockType = rockType,
        geologicalProcesses = geologicalProcesses,
        created = now,
        updated = now
      )
    } catch {
      case _: Exception => throw new BadRequestException("Invalid create sample request")
    }
  }
}

