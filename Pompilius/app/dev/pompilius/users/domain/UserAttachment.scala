package dev.pompilius.users.domain

import dev.pompilius.attachment.domain.AttachmentId
import org.joda.time.DateTime

case class UserAttachment(
    userId: UserId,
    attachmentId: AttachmentId,
    attachmentType: UserAttachmentType,
    createdAt: DateTime,
    isCurrent: Boolean
)
