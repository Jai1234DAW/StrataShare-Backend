package dev.pompilius.sample.domain

import dev.pompilius.resource.domain.ResourceId
import org.joda.time.DateTime

import scala.util.Random

object SampleMother {

  def randomName: String = Random.alphanumeric.take(32).mkString

  def random(resourceId: ResourceId = ResourceId.gen(0)): Sample = {
    val now = DateTime.now

    Sample(
      id = SampleId.gen(0),
      resourceId = resourceId,
      collectedDate = now,
      minerals = Some("Quartz"),
      collectionMethods = Some("Manual collection with basic tools"),
      isFresh = true,
      sampleType = Some("Igneous"),
      materialsUsed = Some("Hammer, shovel, plastic bag"),
      sampleCategory = Some("Rock"),
      geologicalProcesses = Some("Weathering")
    )
  }
}
