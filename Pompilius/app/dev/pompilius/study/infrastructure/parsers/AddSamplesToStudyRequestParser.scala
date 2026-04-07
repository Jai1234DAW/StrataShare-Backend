package dev.pompilius.study.infrastructure.parsers

import dev.pompilius.Strings
import dev.pompilius.sample.domain.SampleId
import dev.pompilius.study.domain.request.AddSamplesToStudyRequest
import dev.pompilius.shared.domain.exceptions.BadRequestException
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Request}

object AddSamplesToStudyRequestParser {

  implicit val jsonReads: Reads[AddSamplesToStudyRequest] =
    (__ \ Strings.sampleIds).read[List[String]].map(_.map(SampleId(_))).map(AddSamplesToStudyRequest.apply)

  def parse[A](request: Request[A]): AddSamplesToStudyRequest = {
    request.body match {
      case AnyContentAsJson(json) =>
        val addSamplesRequest = json.as[AddSamplesToStudyRequest]

        // Para validar que no se envían listas vacías, ya que el parser de JSON no lo hace.
        if (addSamplesRequest.sampleIds.isEmpty) {
          throw new BadRequestException("sampleIds cannot be empty")
        }
        addSamplesRequest

      case _ =>
        throw new BadRequestException("Expecting application/json body")
    }
  }
}
