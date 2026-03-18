package dev.pompilius.attachment.domain

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
    isPublic: Boolean = true,
    deleted: Boolean = false,
    metadata: Option[String]
)
