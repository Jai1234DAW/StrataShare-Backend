package dev.pompilius.shared.infrastructure

import dev.pompilius.shared.domain.exceptions.{
  BadRequestException,
  ForbiddenException,
  NotFoundException,
  InternalServerException
}
import play.api._
import play.api.http.DefaultHttpErrorHandler
import dev.pompilius.shared.infrastructure.writers.VerboseExceptionWriter
import play.api.libs.json.{JsResultException, Json}
import play.api.mvc.{RequestHeader, Result, Results}
import dev.pompilius.shared.domain.VerboseException
import play.api.routing.Router
import dev.pompilius.Strings

import java.sql.SQLException
import javax.inject._
import scala.concurrent.Future

@Singleton
class ErrorHandler @Inject() (
    environment: Environment,
    playConfig: play.api.Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router],
    configuration: dev.pompilius.shared.domain.Configuration,
    verboseExceptionWriter: VerboseExceptionWriter
) extends DefaultHttpErrorHandler(
      environment,
      playConfig,
      sourceMapper,
      router
    ) {

  val logger: Logger = Logger(this.getClass)
  val logAllExceptions: Boolean =
    configuration.logAllExceptions || (environment.mode == Mode.Dev)

  //Sobrescritura de errores en servidor
  override def onDevServerError(
      request: RequestHeader,
      exception: UsefulException
  ): Future[Result] = {
    exceptionToResult(exception)
  }

  override def onProdServerError(
      request: RequestHeader,
      exception: UsefulException
  ): Future[Result] = {
    exceptionToResult(exception)
  }

  //Sobrescritura de errores HTTP específicos
  override def onBadRequest(
      request: RequestHeader,
      message: String
  ): Future[Result] = {
    Future.successful(verboseExceptionWriter.toResult(new BadRequestException()))
  }

  override def onForbidden(
      request: RequestHeader,
      message: String
  ): Future[Result] = {
    Future.successful(verboseExceptionWriter.toResult(new ForbiddenException("Forbidden")))
  }

  override def onNotFound(
      request: RequestHeader,
      message: String
  ): Future[Result] = {
    Future.successful(verboseExceptionWriter.toResult(new NotFoundException("Not found")))
  }

  override def onOtherClientError(
      request: RequestHeader,
      statusCode: Int,
      message: String
  ): Future[Result] = {
    Future.successful(Results.Status(statusCode)(Json.toJson(Json.obj())))
  }

  override def logServerError(
      request: RequestHeader,
      usefulException: UsefulException
  ): Unit = {
    if (logAllExceptions) {
      logger.error(formatServerError(request, usefulException), usefulException)
    } else {
      Option(usefulException.cause).getOrElse(usefulException) match {
        case e: VerboseException if !e.logAsError =>
          logger.debug(
            formatServerError(request, usefulException),
            usefulException
          )
        case _ =>
          logger.error(
            formatServerError(request, usefulException),
            usefulException
          )
      }
    }
  }

  private def exceptionToResult(
      exception: UsefulException
  ): Future[Result] = {

    def replaceException(
        newException: VerboseException
    ): Future[Result] = {
      newException.id = exception.id
      Future.successful(verboseExceptionWriter.toResult(newException))
    }

    Option(exception.cause).getOrElse(exception) match {

      case e: VerboseException =>
        Future.successful(verboseExceptionWriter.toResult(e))

      case _: JsResultException =>
        replaceException(
          new BadRequestException(
            message = "Invalid json value"
          )
        )

      case _: NoSuchElementException =>
        replaceException(
          new BadRequestException(message = "Invalid value")
        )

      case _: UnsupportedOperationException =>
        replaceException(
          new BadRequestException(
            message = "Unsupported operation"
          )
        )

      case _: NumberFormatException =>
        replaceException(
          new BadRequestException(
            message = "Invalid argument"
          )
        )

      case _: SQLException =>
        replaceException(
          new InternalServerException(
            message = "Internal server error"
          )
        )

      case _ =>
        Future.successful(
          verboseExceptionWriter.toResult(
            new VerboseException(
              message = exception.getMessage
            )
          )
        )
    }
  }

  private def formatServerError(
      request: RequestHeader,
      usefulException: UsefulException
  ): String = {
    Option(usefulException.cause).getOrElse(usefulException) match {
      case e: VerboseException =>
        s"""
           |Internal server error for (${request.method}) [${request.uri}]
           |@${e.id} [${request.remoteAddress}] [${request.session.get(Strings.userId).getOrElse("")}]
    """.stripMargin

      case _ =>
        s"""
           |Internal server error for (${request.method}) [${request.uri}]
           |@${usefulException.id} [${request.remoteAddress}] [${request.session.get(Strings.userId).getOrElse("")}]
    """.stripMargin
    }
  }

}
