package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy
import org.apache.pekko.Done

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@ImplementedBy(classOf[RequestLoggerImpl])
trait RequestLogger {
  def log(requestLog: RequestLog): Future[Done]
}

@Singleton
class RequestLoggerImpl @Inject() (
    requestLogRepository: RequestLogRepository
) extends RequestLogger {

  override def log(requestLog: RequestLog): Future[Done] =
    requestLogRepository.save(requestLog)
}
