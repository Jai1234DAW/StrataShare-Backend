package dev.pompilius.resource.infrastructure.writers

import dev.pompilius.Strings
import dev.pompilius.resource.domain.{ResourceAccessLevel, ResourceMother}
import dev.pompilius.sample.domain.SampleMother
import dev.pompilius.shared.infrastructure.ClockMock
import dev.pompilius.users.domain.UserId
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class ResourceWriterUnitTest
    extends PlaySpec
    with ClockMock
    with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global

  "ResourceWriter" must {

    val resourceWriter: ResourceWriter = new ResourceWriterImpl()
    val ownerId = UserId.gen(0)

    "write valid private json for a resource without specific data" in {
      val resource = ResourceMother.random()
      val json = resourceWriter
        .asPrivate(resource, ResourceAccessLevel.OWNER, ownerId)
        .futureValue

      (json \ Strings.resourceId).as[String] mustBe resource.id.toString
      (json \ Strings.accessLevel).as[String] mustBe ResourceAccessLevel.OWNER.toString
      (json \ Strings.ownerId).as[String] mustBe ownerId.toString
      (json \ Strings.sampleData).toOption mustBe None
      (json \ Strings.studyData).toOption mustBe None
    }

    "write sampleData in public mode when sample is provided" in {
      val resource = ResourceMother.random()
      val sample = SampleMother.random(resource.id)

      val json: JsValue = resourceWriter
        .asPublic(
          resource = resource,
          accessLevel = ResourceAccessLevel.OWNER,
          ownerId = ownerId,
          userTypeRelation = Some("OWNER"),
          sample = Some(sample),
          study = None
        )
        .futureValue

      (json \ Strings.sampleData \ Strings.id).as[String] mustBe sample.id.toString
      (json \ Strings.sampleData \ Strings.minerals).as[String] mustBe sample.minerals.get
      (json \ Strings.userTypeRelation).as[String] mustBe "OWNER"
    }
  }
}
