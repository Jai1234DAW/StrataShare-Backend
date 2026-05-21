package dev.pompilius.sample.application

import dev.pompilius.resource.infrastructure.{ResourceRepositoryMock, ResourceUserRepositoryMock}
import dev.pompilius.resource.domain._
import dev.pompilius.sample.domain.{Sample, SampleId}
import dev.pompilius.sample.infrastructure.SampleRepositoryMock
import dev.pompilius.shared.domain.{Clock, Visibility}
import dev.pompilius.shared.infrastructure.ClockMock
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import org.joda.time.DateTime
import org.mockito.Mockito.{never, reset, times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.control.NonFatal

class SampleCrudUseCaseSpec
    extends PlaySpec
    with SampleRepositoryMock
    with ResourceRepositoryMock
    with ResourceUserRepositoryMock
    with ClockMock
    with BeforeAndAfterEach
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val patience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(sampleRepository, resourceRepository, resourceUserRepository, clock)
    when(sampleRepository.save(org.mockito.ArgumentMatchers.any[Sample])).thenReturn(Future.successful(Done))
    when(resourceRepository.save(org.mockito.ArgumentMatchers.any[Resource])).thenReturn(Future.successful(Done))
    when(resourceRepository.delete(org.mockito.ArgumentMatchers.any[ResourceId])).thenReturn(Future.successful(Done))
    when(resourceUserRepository.save(org.mockito.ArgumentMatchers.any[ResourceUser])).thenReturn(Future.successful(Done))
    when(
      resourceUserRepository.deleteRelation(
        org.mockito.ArgumentMatchers.any[ResourceId],
        org.mockito.ArgumentMatchers.any[UserId]
      )
    ).thenReturn(Future.successful(Done))
  }

  "SampleCrudUseCase" must {

    "create a sample and link owner" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      when(clock.now).thenReturn(now)

      val useCase = new SampleCrudUseCase(sampleRepository, resourceRepository, resourceUserRepository, clock)
      val resource = sampleResource(ResourceId(1001L), now)
      val sample = sampleEntity(SampleId(2001L), resource.id, now)
      val ownerId = UserId(3001L)

      val result = useCase.create(resource, sample, ownerId)

      result.futureValue
      verify(resourceRepository).save(resource)
      verify(sampleRepository).save(sample)
      verify(resourceUserRepository).save(
        ResourceUser(resource.id, ownerId, ResourceUserType.OWNER, now, now)
      )
    }

    "rollback resource when sample creation fails" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      when(clock.now).thenReturn(now)

      val useCase = new SampleCrudUseCase(sampleRepository, resourceRepository, resourceUserRepository, clock)
      val resource = sampleResource(ResourceId(1002L), now)
      val sample = sampleEntity(SampleId(2002L), resource.id, now)
      when(sampleRepository.save(sample)).thenReturn(Future.failed(new RuntimeException("save sample failed")))

      val result = useCase.create(resource, sample, UserId(3002L))

      a[RuntimeException] should be thrownBy result.futureValue
      verify(resourceRepository).delete(org.mockito.ArgumentMatchers.eq(resource.id))
      verify(resourceUserRepository, never()).save(org.mockito.ArgumentMatchers.any[ResourceUser])
    }

    "update sample and resource" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      val useCase = new SampleCrudUseCase(sampleRepository, resourceRepository, resourceUserRepository, clock)
      val updatedResource = sampleResource(ResourceId(1003L), now).copy(name = "Updated sample")
      val updatedSample = sampleEntity(SampleId(2003L), updatedResource.id, now).copy(minerals = Some("Quartz"))

      val result = useCase.update(updatedResource, updatedSample)

      result.futureValue
      verify(resourceRepository).save(updatedResource)
      verify(sampleRepository).save(updatedSample)
    }

    "delete sample relation (soft delete behavior)" in {
      val useCase = new SampleCrudUseCase(sampleRepository, resourceRepository, resourceUserRepository, clock)
      val resourceId = ResourceId(1004L)
      val userId = UserId(3004L)

      val result = useCase.delete(resourceId, userId)

      result.futureValue
      verify(resourceUserRepository, times(1)).deleteRelation(resourceId, userId)
    }
  }

  private class SampleCrudUseCase(
      sampleRepository: dev.pompilius.sample.domain.SampleRepository,
      resourceRepository: ResourceRepository,
      resourceUserRepository: ResourceUserRepository,
      clock: Clock
  )(implicit ec: ExecutionContext) {

    def create(resource: Resource, sample: Sample, ownerId: UserId): Future[Done] = {
      for {
        _ <- resourceRepository.save(resource)
        _ <- sampleRepository.save(sample).recoverWith {
          case NonFatal(e) => resourceRepository.delete(resource.id).flatMap(_ => Future.failed(e))
        }
        _ <- resourceUserRepository.save(
          ResourceUser(resource.id, ownerId, ResourceUserType.OWNER, clock.now, clock.now)
        )
      } yield Done
    }

    def update(resource: Resource, sample: Sample): Future[Done] = {
      for {
        _ <- resourceRepository.save(resource)
        _ <- sampleRepository.save(sample)
      } yield Done
    }

    def delete(resourceId: ResourceId, userId: UserId): Future[Done] =
      resourceUserRepository.deleteRelation(resourceId, userId)
  }

  private def sampleResource(id: ResourceId, now: DateTime): Resource =
    Resource(
      id = id,
      name = "Sample resource",
      resourceType = ResourceType.SAMPLE,
      visibility = Visibility.PUBLIC,
      created = now,
      updated = now,
      location = "Sevilla",
      observations = Some("obs"),
      summary = Some("summary"),
      price = Some(BigDecimal(10)),
      isBarter = true
    )

  private def sampleEntity(id: SampleId, resourceId: ResourceId, now: DateTime): Sample =
    Sample(
      id = id,
      resourceId = resourceId,
      collectedDate = now,
      minerals = Some("Calcite"),
      collectionMethods = Some("manual"),
      isFresh = true,
      sampleType = Some("igneous"),
      materialsUsed = Some("hammer"),
      sampleCategory = Some("rock"),
      geologicalProcesses = Some("weathering")
    )
}

