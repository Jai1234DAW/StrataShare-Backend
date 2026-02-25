package dev.pompilius.shared.infrastructure.filters

import org.apache.pekko.stream.Materializer
import org.playframework.cachecontrol.HttpDate
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
    nextFilter(requestHeader).map (
      _.withHeaders(HeaderNames.DATE-> HttpDate.format(HttpDate.now))
    )
  }
}

