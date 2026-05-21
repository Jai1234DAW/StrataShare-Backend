package dev.pompilius.sample.infrastructure.repositories

import dev.pompilius.sample.domain.{Sample, SampleId, SampleMother, SampleRepository}
import org.apache.pekko.Done
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SampleRepositoryMySqlIntegrationTest extends PlaySpec with GuiceOneAppPerSuite{

  "SampleRepositoryMySql" should {

    lazy val sampleRepositoryMySql: SampleRepository = app.injector.instanceOf[SampleMySqlRepository]

    "create a new sample" in {
      val sample = SampleMother.random()
      Await.result[Done](sampleRepositoryMySql.save(sample), 5.seconds) mustBe Done

    }

     "find a sample" in {

       val sample = SampleMother.random()
       Await.result[Done](sampleRepositoryMySql.save(sample), 5.seconds) mustBe Done

       Await
         .result[Option[Sample]](
           sampleRepositoryMySql.findById(sample.id),
           5.seconds
         )
         .map(_.minerals) mustBe Some(sample.minerals)
     }

    "update a sample" in {

       val sample = SampleMother.random()
       Await.result[Done](sampleRepositoryMySql.save(sample), 5.seconds) mustBe Done

       val newSample = SampleMother.random().copy(id = sample.id)
       Await.result[Done](sampleRepositoryMySql.save(newSample), 5.seconds) mustBe Done

       Await
         .result[Option[Sample]](
           sampleRepositoryMySql.findById(sample.id),
           5.seconds
         )
         .map(_.minerals) mustBe Some(newSample.minerals)

     }

    "return None if not found" in {

       val sample = SampleMother.random()
       Await.result[Done](sampleRepositoryMySql.save(sample), 5.seconds) mustBe Done

       Await
         .result[Option[Sample]](
           sampleRepositoryMySql.findById(SampleId.gen(0)),
           5.seconds
         ) mustBe None

     }
  }

}

