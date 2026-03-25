package dev.pompilius.study.domain

import dev.pompilius.attachment.domain.AttachmentId

case class StudyAttachment(
    studyId: StudyId,
    attachmentId: AttachmentId
)

