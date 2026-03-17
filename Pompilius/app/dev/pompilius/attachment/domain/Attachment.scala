package dev.pompilius.attachments.domain

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
    fingerprint: Option[String],
    deleted: Boolean = false,
    metadata: Option[String]
)
