package dev.pompilius.resource.domain.study

import dev.pompilius.resource.domain.sample.SampleId

case class StudySampleFilter(studyId: Option[StudyId] = None, sampleId: Option[SampleId] = None)

