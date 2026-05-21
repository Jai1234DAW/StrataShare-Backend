package dev.pompilius.resource.domain

import org.scalatestplus.play.PlaySpec

class ResourceIdUnitTest  extends PlaySpec{
  "ResourceId" should {

    "create unique and sortable resourceId" in {
      val a = ResourceId.gen(32)
      val b = ResourceId.gen(32)
      val c = ResourceId.gen(32)
      a.id must be < b.id
      b.id must be < c.id
    }

    "parse id from string" in {
      val resourceId = ResourceId.gen(32)
      ResourceId(resourceId.toString) mustBe resourceId
    }
  }
}
