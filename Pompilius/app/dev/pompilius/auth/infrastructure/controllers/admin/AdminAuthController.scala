package dev.pompilius.auth.infrastructure.controllers.admin

import dev.pompilius.Strings
import dev.pompilius.auth.application.SessionCreator
import dev.pompilius.auth.infrastructure.parsers._
import dev.pompilius.auth.infrastructure.writers.SessionWriter
import dev.pompilius.shared.domain._
import dev.pompilius.shared.domain.exceptions.UnauthorizedException
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain._
import play.api.Logger
import play.api.cache.AsyncCacheApi
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminAuthController @Inject()(
    sessionCreator: SessionCreator,
    sessionWriter: SessionWriter,
    userRepository: UserRepository,
    clock: Clock,
    configuration: Configuration,
    requestLogger: RequestLogger,
    implicit val cacheApi: AsyncCacheApi
)(implicit ec: ExecutionContext)
    extends BaseController {

  private val logger = Logger(this.getClass)

  // Con la intención de dar soporte, y solucionar incidencias, a los administradores se les da el permiso de
  // iniciar session como otro usuario, solo necesita conocer su username.
  def loginAs: Action[AnyContent] =
    Action.async { implicit request =>
      findCurrentUser.flatMap {
        case Some(admin) =>
          findRolesByUser(admin).flatMap { roles =>
            if (roles.contains(Role.ADMIN)) {
              val loginAsRequest = LoginAsRequestParser.parse(request)
              for {
                session <- sessionCreator.loginAs(
                  loginAsRequest = loginAsRequest,
                  requestFingerprint = getFingerprint
                )
                user <- userRepository.getById(session.userId)
                userRoles <- findRolesByUser(user)
                userSessionJson <- sessionWriter.asAdmin(session, user, userRoles)
                _ <- requestLogger.log(
                  RequestLog(
                    id = RequestLogId.gen(configuration.nodeId),
                    userId = admin.id,
                    timestamp = clock.now,
                    address = request.remoteAddress,
                    method = request.method,
                    path = request.path,
                    body = request.body.asJson.map(_.toString),
                    metadata = None
                  )
                )
              } yield {
                Ok(userSessionJson)
                  .addingToSession(
                    Strings.userId -> session.userId.toString,
                    Strings.sessionId -> session.id.toString
                  )
              }
            } else {
              throw new UnauthorizedException("You are not allowed to perform this action")
            }
          }
        case None =>
          Future.successful(Unauthorized("No autenticado"))
      }
    }
}
