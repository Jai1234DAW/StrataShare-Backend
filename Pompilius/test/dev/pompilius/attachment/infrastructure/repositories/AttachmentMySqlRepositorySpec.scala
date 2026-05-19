package dev.pompilius.attachment.infrastructure.repositories

import dev.pompilius.attachment.domain.{Attachment, AttachmentId}
import dev.pompilius.resource.domain.ResourceId
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration._

// Test suite for AttachmentMySqlRepository with Mockito
// Tests the behavior of attachment counting by type:
// - When attachmentType is "image", it should count all image/* types
// - When attachmentType is NOT "image", it should count all non-image files
class AttachmentMySqlRepositorySpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  "AttachmentMySqlRepository.countByType with Mockito" should {

    "count only images when attachmentType is 'image'" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val attachmentType = "image"
      val expectedImageCount = 5

      // When: Configure mock to return image count
      when(mockRepo.countByType(resourceId, attachmentType))
        .thenReturn(Future.successful(expectedImageCount))

      // Then: Call the method
      val result = mockRepo.countByType(resourceId, attachmentType)

      // Assert: Verify the count
      whenReady(result) { count =>
        count shouldBe 5
        info(s"✓ Counted $count images (jpeg, png, gif, etc.)")
      }

      // Verify: Method was called with correct parameters
      verify(mockRepo).countByType(resourceId, "image")
    }

    "count all non-image files when attachmentType is NOT 'image'" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val attachmentType = "file"
      val expectedFileCount = 12

      // When: Configure mock to return file count
      when(mockRepo.countByType(resourceId, attachmentType))
        .thenReturn(Future.successful(expectedFileCount))

      // Then: Call the method
      val result = mockRepo.countByType(resourceId, attachmentType)

      // Assert: Verify the count
      whenReady(result) { count =>
        count shouldBe 12
        info(s"✓ Counted $count files (PDFs, videos, documents, etc.)")
      }

      // Verify: Method was called
      verify(mockRepo).countByType(resourceId, "file")
    }

    "return 0 when no attachments exist for resource" in {
      // Given: Mock repository with no attachments
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(999L)

      // When: No attachments exist
      when(mockRepo.countByType(any[ResourceId], any[String]))
        .thenReturn(Future.successful(0))

      // Then: Should return 0
      val result = mockRepo.countByType(resourceId, "image")

      whenReady(result) { count =>
        count shouldBe 0
        info("✓ Returns 0 when resource has no attachments")
      }
    }
  }

  "AttachmentMySqlRepository.findById with Mockito" should {

    "return Some(attachment) when attachment exists" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val attachmentId = AttachmentId(1L)
      val expectedAttachment = createTestAttachment(1L, 123L, "test.jpg", "image/jpeg")

      // When: Attachment exists
      when(mockRepo.findById(attachmentId))
        .thenReturn(Future.successful(Some(expectedAttachment)))

      // Then: Should return the attachment
      val result = mockRepo.findById(attachmentId)

      whenReady(result) { attachmentOpt =>
        attachmentOpt shouldBe defined
        attachmentOpt.get.id shouldBe attachmentId
        attachmentOpt.get.filename shouldBe "test.jpg"
        info("✓ Successfully retrieved attachment by ID")
      }

      verify(mockRepo).findById(attachmentId)
    }

    "return None when attachment does not exist" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val attachmentId = AttachmentId(999L)

      // When: Attachment doesn't exist
      when(mockRepo.findById(attachmentId))
        .thenReturn(Future.successful(None))

      // Then: Should return None
      val result = mockRepo.findById(attachmentId)

      whenReady(result) { attachmentOpt =>
        attachmentOpt shouldBe None
        info("✓ Returns None for non-existent attachment")
      }

      verify(mockRepo).findById(attachmentId)
    }
  }

  "AttachmentMySqlRepository.findByResourceId with Mockito" should {

    "return list of attachments for a resource" in {
      // Given: Mock repository with attachments
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val pagination = dev.pompilius.shared.domain.Pagination(0, Some(10))

      val attachments = List(
        createTestAttachment(1L, 123L, "image1.jpg", "image/jpeg"),
        createTestAttachment(2L, 123L, "image2.png", "image/png"),
        createTestAttachment(3L, 123L, "doc.pdf", "application/pdf")
      )

      // When: Mock returns attachments
      when(mockRepo.findByResourceId(resourceId, pagination))
        .thenReturn(Future.successful(attachments))

      // Then: Should return all attachments
      val result = mockRepo.findByResourceId(resourceId, pagination)

      whenReady(result) { list =>
        list should have size 3
        list.head.filename shouldBe "image1.jpg"
        list(1).filename shouldBe "image2.png"
        list(2).filename shouldBe "doc.pdf"
        info("✓ Retrieved all non-deleted attachments ordered by ID DESC")
      }

      verify(mockRepo).findByResourceId(resourceId, pagination)
    }

    "filter out deleted attachments" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val pagination = dev.pompilius.shared.domain.Pagination(0, Some(10))

      // Only non-deleted attachments
      val nonDeletedAttachments = List(
        createTestAttachment(1L, 123L, "active.jpg", "image/jpeg", deleted = false)
      )

      // When: Mock filters deleted attachments
      when(mockRepo.findByResourceId(resourceId, pagination))
        .thenReturn(Future.successful(nonDeletedAttachments))

      // Then: Should only return non-deleted
      val result = mockRepo.findByResourceId(resourceId, pagination)

      whenReady(result) { list =>
        list should have size 1
        list.forall(_.deleted == false) shouldBe true
        info("✓ Only returns attachments where deleted = false")
      }
    }
  }

  "AttachmentMySqlRepository.findPreviewImageByResourceId with Mockito" should {

    "return the preview image for a resource" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val previewImage = createTestAttachment(
        5L, 123L, "preview.jpg", "image/jpeg", previewImage = true
      )

      // When: Preview image exists
      when(mockRepo.findPreviewImageByResourceId(resourceId))
        .thenReturn(Future.successful(Some(previewImage)))

      // Then: Should return preview image
      val result = mockRepo.findPreviewImageByResourceId(resourceId)

      whenReady(result) { imageOpt =>
        imageOpt shouldBe defined
        imageOpt.get.previewImage shouldBe true
        imageOpt.get.filename shouldBe "preview.jpg"
        info("✓ Retrieved the preview image (previewImage = true)")
      }

      verify(mockRepo).findPreviewImageByResourceId(resourceId)
    }

    "return None when no preview image exists" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)

      // When: No preview image set
      when(mockRepo.findPreviewImageByResourceId(resourceId))
        .thenReturn(Future.successful(None))

      // Then: Should return None
      val result = mockRepo.findPreviewImageByResourceId(resourceId)

      whenReady(result) { imageOpt =>
        imageOpt shouldBe None
        info("✓ Returns None when no preview image is set")
      }
    }
  }

  "AttachmentMySqlRepository.delete with Mockito" should {

    "perform soft delete on attachment" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val attachmentId = AttachmentId(1L)

      // When: Delete is called
      when(mockRepo.delete(attachmentId))
        .thenReturn(Future.successful(org.apache.pekko.Done))

      // Then: Should complete successfully
      val result = mockRepo.delete(attachmentId)

      whenReady(result) { done =>
        done shouldBe org.apache.pekko.Done
        info("✓ Soft delete completed (sets deleted = true)")
      }

      verify(mockRepo, times(1)).delete(attachmentId)
    }
  }

  "AttachmentMySqlRepository.setPreviewImageByResourceId with Mockito" should {

    "set an attachment as preview image" in {
      // Given: Mock repository
      val mockRepo = mock[AttachmentMySqlRepository]
      val resourceId = ResourceId(123L)
      val attachmentId = AttachmentId(5L)

      // When: Set preview image
      when(mockRepo.setPreviewImageByResourceId(resourceId, attachmentId))
        .thenReturn(Future.successful(org.apache.pekko.Done))

      // Then: Should complete successfully
      val result = mockRepo.setPreviewImageByResourceId(resourceId, attachmentId)

      whenReady(result) { done =>
        done shouldBe org.apache.pekko.Done
        info("✓ Set previewImage = true for the specified attachment")
      }

      verify(mockRepo).setPreviewImageByResourceId(resourceId, attachmentId)
    }
  }

  // Helper method to create test Attachment
  private def createTestAttachment(
      id: Long,
      resourceId: Long,
      filename: String,
      contentType: String,
      deleted: Boolean = false,
      previewImage: Boolean = false
  ): Attachment = {
    Attachment(
      id = AttachmentId(id),
      node = 0,
      relativePath = s"/path/to/$filename",
      filename = filename,
      description = Some(s"Test file: $filename"),
      contentType = contentType,
      size = 1024L,
      createdAt = DateTime.now(),
      deleted = deleted,
      metadata = None,
      resourceId = Some(ResourceId(resourceId)),
      previewImage = previewImage
    )
  }
}

