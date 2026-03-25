package dev.pompilius.study.domain

import dev.pompilius.attachment.domain.AttachmentId

case class StudyAttachmentFilter(
    studyId: Option[StudyId] = None,
    attachmentId: Option[AttachmentId] = None
)

