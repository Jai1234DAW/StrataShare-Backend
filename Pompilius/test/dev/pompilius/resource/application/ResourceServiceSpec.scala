package dev.pompilius.resource.application

import dev.pompilius.attachment.domain.{Attachment, AttachmentId, AttachmentRepository}
import dev.pompilius.resource.domain.ResourceId
import dev.pompilius.shared.domain.Pagination
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.apache.pekko.Done

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Test suite demonstrating Mockito usage for testing ResourceService
 *
 * This test shows how to:
 * - Mock repository dependencies
 * - Verify method calls
 * - Test async operations with Future
 * - Use argument matchers
 **/
class ResourceServiceSpec
    extends AnyWordSpec
    with Matchers
    with MockitoSugar
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  "ResourceService with mocked AttachmentRepository" should {

    "count images correctly using the repository" in {
      // Given: Create a mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(123L)
      val expectedImageCount = 5

      // When: Configure the mock to return expected value
      when(mockAttachmentRepo.countByType(any[ResourceId], any[String]))
        .thenReturn(Future.successful(expectedImageCount))

      // Then: Call the method
      val result = mockAttachmentRepo.countByType(resourceId, "image")

      // Verify: Check the result
      whenReady(result) { count =>
        count shouldBe 5
      }

      // Verify: The repository method was called with correct arguments
      verify(mockAttachmentRepo, times(1)).countByType(resourceId, "image")
    }

    "count files (non-images) correctly using the repository" in {
      // Given: Create a mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(123L)
      val expectedFileCount = 12

      // When: Configure the mock to return expected value
      when(mockAttachmentRepo.countByType(any[ResourceId], anyString()))
        .thenReturn(Future.successful(expectedFileCount))

      // Then: Call the method0----------------------------------------
      val result = mockAttachmentRepo.countByType(resourceId, "file")

      // Verify: Check the result
      whenReady(result) { count =>
        count shouldBe 12
      }

      // Verify: The repository method was called exactly once with "file"
      verify(mockAttachmentRepo).countByType(resourceId, "file")
    }

    "return attachments list when calling findByResourceId" in {
      // Given: Create a mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(456L)
      val pagination = Pagination(0, Some(10))

      val mockAttachments = List(
        createMockAttachment(1L, "image1.jpg", "image/jpeg"),
        createMockAttachment(2L, "image2.png", "image/png"),
        createMockAttachment(3L, "document.pdf", "application/pdf")
      )

      // When: Configure the mock to return the list
      when(mockAttachmentRepo.findByResourceId(any[ResourceId], any[Pagination]))
        .thenReturn(Future.successful(mockAttachments))

      // Then: Call the repository method
      val result = mockAttachmentRepo.findByResourceId(resourceId, pagination)

      // Verify: Check the result contains 3 attachments
      whenReady(result) { attachments =>
        attachments should have size 3
        attachments.head.filename shouldBe "image1.jpg"
        attachments(1).filename shouldBe "image2.png"
        attachments(2).filename shouldBe "document.pdf"
      }

      // Verify: Method was called with correct parameters
      verify(mockAttachmentRepo).findByResourceId(resourceId, pagination)
    }

    "find attachment by ID" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val attachmentId = AttachmentId(789L)
      val expectedAttachment = createMockAttachment(789L, "test.jpg", "image/jpeg")

      // When: Mock returns the attachment
      when(mockAttachmentRepo.findById(any[AttachmentId]))
        .thenReturn(Future.successful(Some(expectedAttachment)))

      // Then: Find the attachment
      val result = mockAttachmentRepo.findById(attachmentId)

      // Verify: Attachment is found
      whenReady(result) { attachmentOpt =>
        attachmentOpt shouldBe defined
        attachmentOpt.get.id.id shouldBe 789L
        attachmentOpt.get.filename shouldBe "test.jpg"
        attachmentOpt.get.contentType shouldBe "image/jpeg"
      }

      // Verify: Method was called
      verify(mockAttachmentRepo).findById(attachmentId)
    }

    "return None when attachment not found" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val attachmentId = AttachmentId(999L)

      // When: Mock returns None (not found)
      when(mockAttachmentRepo.findById(any[AttachmentId]))
        .thenReturn(Future.successful(None))

      // Then: Try to find non-existent attachment
      val result = mockAttachmentRepo.findById(attachmentId)

      // Verify: None is returned
      whenReady(result) { attachmentOpt =>
        attachmentOpt shouldBe None
      }

      // Verify: Method was called
      verify(mockAttachmentRepo).findById(attachmentId)
    }

    "delete attachment (soft delete)" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val attachmentId = AttachmentId(100L)

      // When: Mock returns Done
      when(mockAttachmentRepo.delete(any[AttachmentId]))
        .thenReturn(Future.successful(Done))

      // Then: Delete the attachment
      val result = mockAttachmentRepo.delete(attachmentId)

      // Verify: Operation completed
      whenReady(result) { done =>
        done shouldBe Done
      }

      // Verify: Delete was called exactly once
      verify(mockAttachmentRepo, times(1)).delete(attachmentId)
    }

    "save attachment successfully" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val attachment = createMockAttachment(1L, "newfile.pdf", "application/pdf")

      // When: Mock returns Done
      when(mockAttachmentRepo.save(any[Attachment]))
        .thenReturn(Future.successful(Done))

      // Then: Save the attachment
      val result = mockAttachmentRepo.save(attachment)

      // Verify: Save completed
      whenReady(result) { done =>
        done shouldBe Done
      }

      // Verify: Save was called with the attachment
      verify(mockAttachmentRepo).save(attachment)
    }

    "find preview image for a resource" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(123L)
      val previewImage = createMockAttachment(
        id = 5L,
        filename = "preview.jpg",
        contentType = "image/jpeg",
        previewImage = true
      )

      // When: Mock returns the preview image
      when(mockAttachmentRepo.findPreviewImageByResourceId(any[ResourceId]))
        .thenReturn(Future.successful(Some(previewImage)))

      // Then: Get preview image
      val result = mockAttachmentRepo.findPreviewImageByResourceId(resourceId)

      // Verify: Preview image is found
      whenReady(result) { imageOpt =>
        imageOpt shouldBe defined
        imageOpt.get.previewImage shouldBe true
        imageOpt.get.filename shouldBe "preview.jpg"
      }

      // Verify: Method was called
      verify(mockAttachmentRepo).findPreviewImageByResourceId(resourceId)
    }

    "set preview image for a resource" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(123L)
      val attachmentId = AttachmentId(7L)

      // When: Mock returns Done
      when(mockAttachmentRepo.setPreviewImageByResourceId(any[ResourceId], any[AttachmentId]))
        .thenReturn(Future.successful(Done))

      // Then: Set preview image
      val result = mockAttachmentRepo.setPreviewImageByResourceId(resourceId, attachmentId)

      // Verify: Operation completed
      whenReady(result) { done =>
        done shouldBe Done
      }

      // Verify: Method was called with correct parameters
      verify(mockAttachmentRepo).setPreviewImageByResourceId(resourceId, attachmentId)
    }

    "verify multiple interactions with repository" in {
      // Given: Mock repository
      val mockAttachmentRepo = mock[AttachmentRepository]
      val resourceId = ResourceId(123L)

      // Configure mocks
      when(mockAttachmentRepo.countByType(any[ResourceId], anyString()))
        .thenReturn(Future.successful(5), Future.successful(10))

      // When: Call multiple times
      val imageCount = mockAttachmentRepo.countByType(resourceId, "image")
      val fileCount = mockAttachmentRepo.countByType(resourceId, "file")

      // Verify: Both calls return expected values
      whenReady(imageCount) { count => count shouldBe 5 }
      whenReady(fileCount) { count => count shouldBe 10 }

      // Verify: Both methods were called
      verify(mockAttachmentRepo).countByType(resourceId, "image")
      verify(mockAttachmentRepo).countByType(resourceId, "file")
      verify(mockAttachmentRepo, times(2)).countByType(any[ResourceId], any[String])
    }

    "handle exceptions from repository" in {
      // Given: Mock repository that throws exception
      val mockAttachmentRepo = mock[AttachmentRepository]
      val attachmentId = AttachmentId(1L)

      // When: Mock throws exception
      when(mockAttachmentRepo.findById(any[AttachmentId]))
        .thenReturn(Future.failed(new RuntimeException("Database connection failed")))

      // Then: Call the method
      val result = mockAttachmentRepo.findById(attachmentId)

      // Verify: Exception is propagated
      whenReady(result.failed) { exception =>
        exception shouldBe a[RuntimeException]
        exception.getMessage shouldBe "Database connection failed"
      }

      // Verify: Method was called
      verify(mockAttachmentRepo).findById(attachmentId)
    }
  }

  // Helper method to create mock Attachment
  private def createMockAttachment(
      id: Long,
      filename: String,
      contentType: String,
      previewImage: Boolean = false
  ): Attachment = {
    Attachment(
      id = AttachmentId(id),
      node = 0,
      relativePath = s"/test/path/$filename",
      filename = filename,
      description = Some(s"Test attachment: $filename"),
      contentType = contentType,
      size = 1024L * 100, // 100 KB
      createdAt = DateTime.now(),
      deleted = false,
      metadata = None,
      resourceId = Some(ResourceId(123L)),
      previewImage = previewImage
    )
  }
}

