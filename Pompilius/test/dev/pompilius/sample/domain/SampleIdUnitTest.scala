package dev.pompilius.sample.domain

import org.scalatestplus.play.PlaySpec

class SampleIdUnitTest extends PlaySpec {

  "SampleId" should {

    "create unique and sortable sampleId" in {
      val a = SampleId.gen(32)
      val b = SampleId.gen(32)
      val c = SampleId.gen(32)
      a.id must be < b.id
      b.id must be < c.id
    }

    "parse id from string" in {
      val sampleId = SampleId.gen(32)
      SampleId(sampleId.toString) mustBe sampleId
    }
  }
}
