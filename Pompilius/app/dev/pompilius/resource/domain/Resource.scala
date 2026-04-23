package dev.pompilius.resource.domain

import dev.pompilius.attachment.domain.AttachmentId
import dev.pompilius.shared.domain.Visibility
import org.joda.time.DateTime

case class Resource(
    id: ResourceId,
    name: String,
    resourceType: ResourceType,
    visibility: Visibility,
    created: DateTime,
    updated: DateTime,
    localization: String,
    observations: Option[String],
    summary: Option[String],
    price: Option[BigDecimal] = None,
    isBarter: Boolean = false,
)
