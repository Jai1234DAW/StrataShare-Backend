package dev.pompilius.resource.domain

import dev.pompilius.attachment.domain.AttachmentId

case class ResourceAttachmentFilter(
    resourceId: Option[ResourceId] = None,
    attachmentId: Option[AttachmentId] = None
)

