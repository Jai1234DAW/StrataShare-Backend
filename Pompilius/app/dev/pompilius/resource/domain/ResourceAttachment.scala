package dev.pompilius.resource.domain

import dev.pompilius.attachment.domain.AttachmentId

case class ResourceAttachment (
    resourceId: ResourceId,
    attachmentId: AttachmentId)
