package dev.pompilius.context.infrastructure.controllers

import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeWrites, toJsValueWrapper}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.Strings
import play.api.libs.json._
import play.api.mvc._
import javax.inject._
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContextController @Inject() (implicit val ec: ExecutionContext) extends BaseController {

  @SuppressWarnings(Array("UnusedMethodParameter"))
  def getContext: Action[AnyContent] =
    Action.async { implicit request =>
      Future.successful {
        Ok(
          Json.obj(
            Seq(
              toJsValueWrapper(Strings.environment, configuration.environment),
              toJsValueWrapper(Strings.address, request.remoteAddress),
              toJsValueWrapper(Strings.language, getLanguage.language),
              toJsValueWrapper(Strings.availableLanguages, langs.availables.map(_.language)),
              toJsValueWrapper(Strings.currentDateTime, clock.now),
              toJsValueWrapper(Strings.parameters, configuration.context.parameters)
            ).flatten: _*
          )
        )
      }
    }
}

