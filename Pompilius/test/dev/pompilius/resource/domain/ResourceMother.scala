package dev.pompilius.resource.domain

import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

import scala.util.Random

object ResourceMother {

  def randomName: String = Random.alphanumeric.take(32).mkString

  def random(
      name: String = randomName,
      resourceType: ResourceType = ResourceType.SAMPLE,
      visibility: Visibility = Visibility.PRIVATE
  ): Resource = {
    val now = DateTime.now
    Resource(
      id = ResourceId.gen(0),
      name = name,
      resourceType = resourceType,
      visibility = visibility,
      created = now,
      updated = now,
      location = "Los Andes, Mérida",
      observations = None,
      summary = Some("This is a sample resource for testing purposes."),
      price = Some(BigDecimal("2.00")),
      isBarter = false
    )
  }
}
