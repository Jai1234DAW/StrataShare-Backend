package dev.pompilius.study.domain.exceptions

import dev.pompilius.shared.domain.exceptions.NotFoundException

class StudyNotFoundException(message: String = "Study not found") extends VerboseException(message = message)

object StudyNotFoundException {
  def apply(studyId: StudyId): UserNotFoundException = {
    new UserNotFoundException(s"Study with id=${studyId.toString} not found")
  }
}
