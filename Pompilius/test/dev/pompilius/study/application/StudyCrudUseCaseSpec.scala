package dev.pompilius.study.application

import dev.pompilius.resource.infrastructure.{ResourceRepositoryMock, ResourceUserRepositoryMock}
import dev.pompilius.resource.domain._
import dev.pompilius.shared.domain.{Clock, Visibility}
import dev.pompilius.shared.infrastructure.ClockMock
import dev.pompilius.study.domain.{Area, Study, StudyId}
import dev.pompilius.study.infrastructure.StudyRepositoryMock
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

class StudyCrudUseCaseSpec
    extends PlaySpec
    with StudyRepositoryMock
    with ResourceRepositoryMock
    with ResourceUserRepositoryMock
    with ClockMock
    with BeforeAndAfterEach
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val patience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(studyRepository, resourceRepository, resourceUserRepository, clock)
    when(studyRepository.save(org.mockito.ArgumentMatchers.any[Study])).thenReturn(Future.successful(Done))
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

  "StudyCrudUseCase" must {

    "create a study and link owner" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      when(clock.now).thenReturn(now)

      val useCase = new StudyCrudUseCase(studyRepository, resourceRepository, resourceUserRepository, clock)
      val resource = studyResource(ResourceId(1101L), now)
      val study = studyEntity(StudyId.gen(0), resource.id, now)
      val ownerId = UserId.gen(0)

      val result = useCase.create(resource, study, ownerId)

      result.futureValue
      verify(resourceRepository).save(resource)
      verify(studyRepository).save(study)
      verify(resourceUserRepository).save(
        ResourceUser(resource.id, ownerId, ResourceUserType.OWNER, now, now)
      )
    }

    "rollback resource when study creation fails" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      when(clock.now).thenReturn(now)

      val useCase = new StudyCrudUseCase(studyRepository, resourceRepository, resourceUserRepository, clock)
      val resource = studyResource(ResourceId(1102L), now)
      val study = studyEntity(StudyId(2102L), resource.id, now)
      when(studyRepository.save(study)).thenReturn(Future.failed(new RuntimeException("save study failed")))

      val result = useCase.create(resource, study, UserId(3102L))

      a[RuntimeException] should be thrownBy result.futureValue
      verify(resourceRepository).delete(org.mockito.ArgumentMatchers.eq(resource.id))
      verify(resourceUserRepository, never()).save(org.mockito.ArgumentMatchers.any[ResourceUser])
    }

    "update study and resource" in {
      val now = DateTime.parse("2026-05-20T12:00:00Z")
      val useCase = new StudyCrudUseCase(studyRepository, resourceRepository, resourceUserRepository, clock)
      val updatedResource = studyResource(ResourceId(1103L), now).copy(name = "Updated study")
      val updatedStudy = studyEntity(StudyId(2103L), updatedResource.id, now).copy(description = "updated")

      val result = useCase.update(updatedResource, updatedStudy)

      result.futureValue
      verify(resourceRepository).save(updatedResource)
      verify(studyRepository).save(updatedStudy)
    }

    "delete study relation (soft delete behavior)" in {
      val useCase = new StudyCrudUseCase(studyRepository, resourceRepository, resourceUserRepository, clock)
      val resourceId = ResourceId(1104L)
      val userId = UserId(3104L)

      val result = useCase.delete(resourceId, userId)

      result.futureValue
      verify(resourceUserRepository, times(1)).deleteRelation(resourceId, userId)
    }
  }

  private class StudyCrudUseCase(
      studyRepository: dev.pompilius.study.domain.StudyRepository,
      resourceRepository: ResourceRepository,
      resourceUserRepository: ResourceUserRepository,
      clock: Clock
  )(implicit ec: ExecutionContext) {

    def create(resource: Resource, study: Study, ownerId: UserId): Future[Done] = {
      for {
        _ <- resourceRepository.save(resource)
        _ <- studyRepository.save(study).recoverWith {
          case NonFatal(e) => resourceRepository.delete(resource.id).flatMap(_ => Future.failed(e))
        }
        _ <- resourceUserRepository.save(
          ResourceUser(resource.id, ownerId, ResourceUserType.OWNER, clock.now, clock.now)
        )
      } yield Done
    }

    def update(resource: Resource, study: Study): Future[Done] = {
      for {
        _ <- resourceRepository.save(resource)
        _ <- studyRepository.save(study)
      } yield Done
    }

    def delete(resourceId: ResourceId, userId: UserId): Future[Done] =
      resourceUserRepository.deleteRelation(resourceId, userId)
  }

  private def studyResource(id: ResourceId, now: DateTime): Resource =
    Resource(
      id = id,
      name = "Study resource",
      resourceType = ResourceType.STUDY,
      visibility = Visibility.PUBLIC,
      created = now,
      updated = now,
      location = "Madrid",
      observations = Some("obs"),
      summary = Some("summary"),
      price = Some(BigDecimal(20)),
      isBarter = false
    )

  private def studyEntity(id: StudyId, resourceId: ResourceId, now: DateTime): Study =
    Study(
      id = id,
      resourceId = resourceId,
      startDate = now,
      endDate = None,
      description = "study description",
      coordinates = "37.3891,-5.9845",
      area = Area.STRATIGRAPHY,
      methods = "field mapping",
      authors = "author",
      section = true,
      antecedents = false,
      nameSection = Some("Section A")
    )
}

