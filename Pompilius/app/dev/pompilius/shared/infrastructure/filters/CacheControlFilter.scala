package dev.pompilius.shared.infrastructure.filters

import org.apache.pekko.stream.Materializer
import play.api.http._
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CacheControlFilter @Inject() (implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter {

  private val MustRevalidate = "no-cache, no-store, must-revalidate"

  def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {

    nextFilter(requestHeader).map { result =>
      result.body.contentType match {
        case _ if !requestHeader.method.equalsIgnoreCase(HttpVerbs.GET) =>
          result.withHeaders(HeaderNames.CACHE_CONTROL -> MustRevalidate)

        case Some(ContentTypes.JSON) =>
          result.withHeaders(HeaderNames.CACHE_CONTROL -> MustRevalidate)

        case _ if result.header.status != Status.OK =>
          result.withHeaders(HeaderNames.CACHE_CONTROL -> MustRevalidate)

        case Some(contentType) if contentType.startsWith("application/") =>
          result.withHeaders(HeaderNames.CACHE_CONTROL -> MustRevalidate)

        case _ if result.header.headers.contains(HeaderNames.CACHE_CONTROL) =>
          result

        case _ =>
          result
      }
    }
  }
}
