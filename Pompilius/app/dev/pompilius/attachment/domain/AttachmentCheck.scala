package dev.pompilius.attachment.domain

import com.google.inject.ImplementedBy
import dev.pompilius.attachment.domain.exceptions.AttachmentNotFoundException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AttachmentCheckImpl])
trait AttachmentCheck {
  def check(attachmentId: AttachmentId): Future[Unit]
  def check(attachmentId: Option[AttachmentId]): Future[Unit]
}

@Singleton
class AttachmentCheckImpl @Inject() (
    attachmentRepository: AttachmentRepository
)(implicit val ec: ExecutionContext)
    extends AttachmentCheck {

  override def check(attachmentId: AttachmentId): Future[Unit] = {
    attachmentRepository
      .findById(attachmentId)
      .map {
        case Some(_) => ()
        case None    => throw AttachmentNotFoundException(attachmentId)
      }
  }

  override def check(attachmentId: Option[AttachmentId]): Future[Unit] = {
    attachmentId match {
      case Some(id) =>
        check(id)
      case _ =>
        Future.unit
    }
  }
}
