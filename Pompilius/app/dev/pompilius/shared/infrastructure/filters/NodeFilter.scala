package dev.pompilius.shared.infrastructure.filters

import org.apache.pekko.stream.Materializer
import play.api.http._
import play.api.mvc._
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import dev.pompilius.Strings

@Singleton
class NodeFilter @Inject() (Configuration: Configuration)(implicit
    val mat: Materializer,
    ec: ExecutionContext
) extends Filter {

  def apply(
      nextFilter: RequestHeader => Future[Result]
  )(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map (
      _.withHeaders(Strings.X_NODE_ID -> Configuration.nodeId.toString))

  }
}