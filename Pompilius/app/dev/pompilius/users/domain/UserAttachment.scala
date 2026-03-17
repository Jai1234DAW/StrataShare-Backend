package dev.pompilius.users.domain

import dev.pompilius.attachment.domain.AttachmentId

case class UserAttachment(userId: UserId, attachmentId: AttachmentId)
