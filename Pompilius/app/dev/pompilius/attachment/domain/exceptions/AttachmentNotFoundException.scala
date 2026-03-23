package dev.pompilius.attachment.domain.exceptions

import dev.pompilius.shared.domain.VerboseException
import dev.pompilius.attachment.domain.AttachmentId

class AttachmentNotFoundException(message: String = "Image not found") extends VerboseException(message = message)

object AttachmentNotFoundException{
  def apply(attachmentId: AttachmentId): AttachmentNotFoundException = {
    new AttachmentNotFoundException(s"Attachment with id=${attachmentId.toString} not found")
  }
}
