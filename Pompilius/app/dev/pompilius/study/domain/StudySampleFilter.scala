package dev.pompilius.study.domain

import dev.pompilius.sample.domain.SampleId

case class StudySampleFilter(studyId: Option[StudyId] = None, sampleId: Option[SampleId] = None)

