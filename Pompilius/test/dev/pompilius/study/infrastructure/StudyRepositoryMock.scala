package dev.pompilius.study.infrastructure

import dev.pompilius.study.domain.{Study, StudyRepository}
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

trait StudyRepositoryMock extends MockitoSugar {

  protected val studyRepository: StudyRepository = mock[StudyRepository]

  when(studyRepository.save(any[Study]())).thenReturn(Future.successful(Done))
}

