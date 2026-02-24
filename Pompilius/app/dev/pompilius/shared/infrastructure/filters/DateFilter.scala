package dev.pompilius.shared.infrastructure.filters

import org.apache.pekko.stream.Materializer
import org.playframework.cacheControl.HttpDate
import play.api.http.HeaderNames
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DateFilter @Inject() (implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter {

  def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      val currentDate = HttpDate.format(System.currentTimeMillis())
      result.withHeaders(HeaderNames.DATE -> currentDate)
    }
  }
}