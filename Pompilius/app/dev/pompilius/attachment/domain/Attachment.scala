package dev.pompilius.attachment.domain

import dev.pompilius.resource.domain.ResourceId
import org.joda.time.DateTime

case class Attachment(
    id: AttachmentId,
    node: Int,
    relativePath: String,
    filename: String,
    description: Option[String],
    contentType: String,
    size: Long,
    createdAt: DateTime,
    deleted: Boolean = false,
    metadata: Option[String],
    resourceId: Option[ResourceId] = None,
    previewImage:Boolean=false
)
