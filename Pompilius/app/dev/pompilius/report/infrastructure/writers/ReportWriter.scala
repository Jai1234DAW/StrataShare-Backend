package dev.pompilius.report.infrastructure.writers

import com.google.inject.ImplementedBy

import javax.inject.{Inject, Singleton}
import dev.pompilius.Strings
import dev.pompilius.report.domain.{Parameter, Report}
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.users.domain.UserRepository
import dev.pompilius.users.infrastructure.writers.UserWriter
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[ReportWriterImpl])
trait ReportWriter {
  def toJson(report: Report): Future[JsValue]
}

@Singleton
class ReportWriterImpl @Inject() (
    userRepository: UserRepository,
    userWriter: UserWriter
)(implicit ec: ExecutionContext)
    extends ReportWriter {

  implicit val reportParameterJsFormat: Format[Parameter] = Json.format[Parameter]

  override def toJson(report: Report): Future[JsValue] = {
    for {
      authorizedUsers <-
        Future
          .sequence(report.authorizedUsers.map { userId =>
            userRepository.findById(userId)
          })
          .map(_.flatten)

      authorizedUsersJs <- Future.sequence(authorizedUsers.map { user =>
        userWriter.toJson(user)
      })

      parameters = report.sheets.flatMap(_.parameters).distinct

    } yield {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.id, report.id.toString),
            toJsValueWrapper(Strings.name, report.name),
            toJsValueWrapper(Strings.title, report.title),
            toJsValueWrapper(Strings.title, report.title),
            toJsValueWrapper(Strings.authorizedUsers, authorizedUsersJs),
            toJsValueWrapper(Strings.parameters, parameters)
          ).flatten: _*
        )
      )
    }
  }

}
