package dev.pompilius.users.infrastructure.writers

import com.google.inject.ImplementedBy
import play.api.libs.json._
import dev.pompilius.users.domain.User
import dev.pompilius.Strings
import dev.pompilius.attachment.domain.AttachmentRepository
import dev.pompilius.country.infrastructure.writers.{CountryWriter, CountryWriterImpl}
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UserWriterImpl])
trait UserWriter {
  def asAdmin(user: User): Future[JsValue]
  def asCurrentUser(user: User): Future[JsValue]
}

@Singleton
class UserWriterImpl @Inject() (
    userRoleRepository: UserRoleRepository,
    attachmentRepository: AttachmentRepository,
    roleWriter: RoleWriter,
    countryWriter: CountryWriter,
    attachmentWriter: AttachmentWriter
)(implicit ec: ExecutionContext)
    extends UserWriter {
  private def base(user: User): Future[JsObject] =
    for {
      // Obtener el JSON del country
      countryJson <- countryWriter.toJson(user.country)

      // Obtener el JSON del avatar si existe
      avatarJs <- user.avatar match {
        case Some(attachmentId) =>
          attachmentRepository.findById(attachmentId).flatMap {
            case Some(image) => Writer.toJson(image).map(Some(_))
            case None        => Future.successful(None)
          }
        case None => Future.successful(None)
      }

    } yield {
      // Construimos el JSON final en una variable antes del yield
      val finalJson = Json.obj(
        List(
          toJsValueWrapper(Strings.id, user.id.toString),
          toJsValueWrapper(Strings.username, user.username),
          toJsValueWrapper(Strings.email, user.email),
          toJsValueWrapper(Strings.phone, user.phone),
          toJsValueWrapper(Strings.avatar, avatarJs), // se usa aquí usamos el avatar resuelto
          toJsValueWrapper(Strings.firstName, user.firstName),
          toJsValueWrapper(Strings.lastName, user.lastName),
          toJsValueWrapper(Strings.country, countryJson), // usamos aqui country resuelto
          toJsValueWrapper(Strings.language, user.language.map(_.language)),
          toJsValueWrapper(Strings.bio, user.bio)
        ).flatten: _*
      )

      finalJson
    }

  // Json para enviar a un administrador
  override def asAdmin(user: User): Future[JsValue] = {
    for {
      baseJson <- base(user)
    } yield {
      baseJson ++ Json.obj(
        List(
          toJsValueWrapper(Strings.enabled, user.enabled),
          toJsValueWrapper(Strings.created, user.created),
          toJsValueWrapper(Strings.updated, user.updated)
        ).flatten: _*
      )
    }
  }
}
