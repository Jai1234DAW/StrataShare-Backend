package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy
import dev.pompilius.shared.infrastructure.repositories.RequestLogMySqlRepository
import org.apache.pekko.Done

import scala.concurrent.Future

@ImplementedBy(classOf[RequestLogMySqlRepository])
trait RequestLogRepository {

  def save(requestLog: RequestLog): Future[Done]

}
