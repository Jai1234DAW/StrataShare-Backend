package dev.pompilius.users.domain

import dev.pompilius.attachment.domain.AttachmentId

case class UserAttachmentFilter(
    userId: Option[UserId] = None,
    attachmentId: Option[AttachmentId] = None,
    attachmentType: Option[UserAttachmentType] = None,
    isCurrent: Option[Boolean] = None
)
