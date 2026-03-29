package dev.pompilius.resource.domain.request

import dev.pompilius.resource.domain.study.Area
import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

// Data classes para Sample y Study
case class SampleData(
  name: String,
  isFresh: Boolean,
  minerals: Option[String] = None,
  collectionMethods: Option[String] = None,
  sampleType: Option[String] = None,
  materialsUsed: Option[String] = None,
  rockType: Option[String] = None,
  geologicalProcesses: Option[String] = None
)

case class StudyData(
  name: String,
  startDate: DateTime,
  description: String,
  coordinates: String,
  area: Area,
  methods: String,
  authors: String,
  section: Boolean,
  endDate: Option[DateTime] = None,
  antecedents: Boolean,
  nameSection: Option[String] = None
)

// Sealed trait para CreateResourceRequest con campos genéricos
sealed trait CreateResourceRequest {
  def visibility: Visibility
  def localization: String
  def observations: Option[String]
  def summary: Option[String]
}

case class CreateSampleResourceRequest(
  visibility: Visibility,
  localization: String,
  observations: Option[String],
  summary: Option[String],
  sampleData: SampleData
) extends CreateResourceRequest

case class CreateStudyResourceRequest(
  visibility: Visibility,
  localization: String,
  observations: Option[String],
  summary: Option[String],
  studyData: StudyData
) extends CreateResourceRequest
