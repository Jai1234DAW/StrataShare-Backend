package dev.pompilius.attachment.domain

import org.joda.time.DateTime
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Visibility

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
