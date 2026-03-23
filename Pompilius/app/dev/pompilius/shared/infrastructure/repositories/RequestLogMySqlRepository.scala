package dev.pompilius.shared.infrastructure.repositories

import dev.pompilius.shared.domain.{RequestLog, RequestLogRepository}
import dev.pompilius.shared.infrastructure.ScalikeUtil
import dev.pompilius.shared.infrastructure.contexts.DbExecutionContext
import org.apache.pekko.Done
import scalikejdbc._
import scalikejdbc.jodatime.JodaParameterBinderFactory._

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RequestLogMySqlRepository @Inject()(implicit dbExecutionContext: DbExecutionContext)
    extends RequestLogRepository
    with SQLSyntaxSupport[RequestLog] {

  override val tableName = "request_log"

  override def save(requestLog: RequestLog): Future[Done] =
    Future {
      DB.localTx { implicit session =>
        val values = List(
          column.id -> requestLog.id.id,
          column.userId -> requestLog.userId.id,
          column.timestamp -> requestLog.timestamp,
          column.address -> requestLog.address,
          column.method -> requestLog.method,
          column.path -> requestLog.path,
          column.body -> requestLog.body,
          column.metadata -> requestLog.metadata
        )
        withSQL {
          insert
            .into(this)
            .namedValues(values: _*)
            .append(ScalikeUtil.onDuplicateUpdate(values: _*))
        }.update()
      }
      Done
    }

}
