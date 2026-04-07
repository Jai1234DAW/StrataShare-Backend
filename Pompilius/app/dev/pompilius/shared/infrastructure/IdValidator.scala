package dev.pompilius.shared.infrastructure

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.sample.domain.SampleId
import dev.pompilius.study.domain.StudyId
import dev.pompilius.shared.domain.exceptions.BadRequestException


trait IdValidator {

  protected def checkResourceId(idStr: String): ResourceId = {
    try {
      ResourceId(idStr)
    } catch {
      case _: NumberFormatException =>
        throw new BadRequestException(s"Invalid resource ID: $idStr")
      case e: Exception =>
        throw new BadRequestException(s"Invalid resource ID format: ${e.getMessage}")
    }
  }

  protected def checkSampleId(idStr: String): SampleId = {
    try {
      SampleId(idStr)
    } catch {
      case _: NumberFormatException =>
        throw new BadRequestException(s"Invalid sample ID: $idStr")
      case e: Exception =>
        throw new BadRequestException(s"Invalid sample ID format: ${e.getMessage}")
    }
  }

  protected def checkStudyId(idStr: String): StudyId = {
    try {
      StudyId(idStr)
    } catch {
      case _: NumberFormatException =>
        throw new BadRequestException(s"Invalid study ID: $idStr")
      case e: Exception =>
        throw new BadRequestException(s"Invalid study ID format: ${e.getMessage}")
    }
  }

  protected def checkAttachmentId(idStr: String): AttachmentId = {
    try {
      AttachmentId(idStr)
    } catch {
      case _: NumberFormatException =>
        throw new BadRequestException(s"Invalid attachment ID: $idStr")
      case e: Exception =>
        throw new BadRequestException(s"Invalid attachment ID format: ${e.getMessage}")
    }
  }
}

