package dev.pompilius.auth.infrastructure.writers

import dev.pompilius.auth.domain.Session
import com.google.inject.ImplementedBy
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.Strings
import play.api.libs.json._
import dev.pompilius.users.domain.{Role, User}
import dev.pompilius.users.infrastructure.writers.UserWriter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SessionWriterImpl])
trait SessionWriter {
  def toJson(
      session: Session,
      user: User,
      roles: List[Role]
  ): Future[JsValue]

  def asAdmin(session: Session, user: User, roles: List[Role]): Future[JsValue]
}

@Singleton
class SessionWriterImpl @Inject() (userWriter: UserWriter)(implicit ec: ExecutionContext) extends SessionWriter {

  // Json para enviar al usuario autenticado en esta sesión
  override def toJson(session: Session, user: User, roles: List[Role]): Future[JsValue] = {
    for {
      userJson <- userWriter.asCurrentUser(user)
    } yield {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.id, session.id.toString),
            toJsValueWrapper(Strings.createdAt, session.created),
            toJsValueWrapper(Strings.updatedAt, session.updatedAt),
            toJsValueWrapper(Strings.roles, roles.map(_.toString)),
            toJsValueWrapper(Strings.user, userJson)
          ).flatten: _*
        )
      )
    }
  }

  override def asAdmin(session: Session, user: User, roles: List[Role]): Future[JsValue] = {
    for {
      userJson <- userWriter.asAdmin(user)
    } yield {
      Json.toJson(
        Json.obj(
          List(
            toJsValueWrapper(Strings.id, session.id.toString),
            toJsValueWrapper(Strings.createdAt, session.created),
            toJsValueWrapper(Strings.updatedAt, session.updatedAt),
            toJsValueWrapper(Strings.roles, roles.map(_.toString)),
            toJsValueWrapper(Strings.user, userJson)
          ).flatten: _*
        )
      )
    }
  }
}
