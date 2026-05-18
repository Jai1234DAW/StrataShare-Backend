package dev.pompilius.users.infrastructure.writers

import com.google.inject.ImplementedBy
import dev.pompilius.Strings
import dev.pompilius.country.infrastructure.writers.CountryWriter
import dev.pompilius.shared.infrastructure.JsUtils.{JodaDateTimeFormat, toJsValueWrapper}
import dev.pompilius.shared.infrastructure.UrlUtil
import dev.pompilius.users.domain.{User, UserAttachmentRepository, UserAttachmentType, UserRoleRepository}
import play.api.libs.json._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[UserWriterImpl])
trait UserWriter {
  def toJson(user: User): Future[JsValue]
  def asAdmin(user: User): Future[JsValue]
  def asCurrentUser(user: User): Future[JsValue]
  def asAnotherUser(user: User): Future[JsValue]
}

@Singleton
class UserWriterImpl @Inject() (
    userRoleRepository: UserRoleRepository,
    userAttachmentRepository: UserAttachmentRepository,
    countryWriter: CountryWriter
)(implicit ec: ExecutionContext)
    extends UserWriter {
  override def toJson(user: User): Future[JsObject] =
    for {

      // Obtener el JSON del country
      countryJson <- countryWriter.toJson(user.country)

      // Obtener el avatar actual desde UserAttachment
      avatarAttachment <- userAttachmentRepository.findCurrentByType(user.id, UserAttachmentType.AVATAR)
      avatarJs = avatarAttachment.map { attachment =>
        UrlUtil.addQueryParameters(
          dev.pompilius.users.infrastructure.controllers.routes.UserController.downloadAvatar(user.id.toString).url,
          Map("hash" -> attachment.attachmentId.toString) // Cambiamos la url si cambia el avatar (para evitar la caché)
        )
      }

      // Obtener el cover photo actual desde UserAttachment
      coverPhotoAttachment <- userAttachmentRepository.findCurrentByType(user.id, UserAttachmentType.COVER_PHOTO)
      coverPhotoJs = coverPhotoAttachment.map { attachment =>
        UrlUtil.addQueryParameters(
          dev.pompilius.users.infrastructure.controllers.routes.UserController.downloadCoverPhoto(user.id.toString).url,
          Map("hash" -> attachment.attachmentId.toString)
        )
      }

      roles<- userRoleRepository.getAllByUserId(user.id).map(_.map(_.role))

    } yield {
      // Construimos el JSON final en una variable antes del yield
      val finalJson = Json.obj(
        List(
          toJsValueWrapper(Strings.id, user.id.toString),
          toJsValueWrapper(Strings.username, user.username),
          toJsValueWrapper(Strings.email, user.email),
          toJsValueWrapper(Strings.interests, user.interests),
          toJsValueWrapper(Strings.phone, user.phone),
          toJsValueWrapper(Strings.avatar, avatarJs),
          toJsValueWrapper(Strings.coverPhoto,coverPhotoJs),
          toJsValueWrapper(Strings.firstName, user.firstName),
          toJsValueWrapper(Strings.lastName, user.lastName),
          toJsValueWrapper(Strings.country, countryJson),
          toJsValueWrapper(Strings.language, user.language.map(_.language)),
          toJsValueWrapper(Strings.bio, user.bio),
          toJsValueWrapper(Strings.created, user.created),
          toJsValueWrapper(Strings.role, roles)
        ).flatten: _*
      )

      finalJson
    }

  // Json para enviar a un administrador
  override def asAdmin(user: User): Future[JsValue] = {
    for {
      baseJson <- toJson(user)
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

  override def asCurrentUser(user: User): Future[JsValue] = {
    for {
      baseJson <- toJson(user)
    } yield {
      baseJson
    }
  }

  override def asAnotherUser(user: User): Future[JsValue] = {
    for {
      // Obtener el JSON del country
      countryJson <- countryWriter.toJson(user.country)

      // Obtener el avatar actual desde UserAttachment
      avatarAttachment <- userAttachmentRepository.findCurrentByType(user.id, UserAttachmentType.AVATAR)
      avatarJs = avatarAttachment.map { attachment =>
        UrlUtil.addQueryParameters(
          dev.pompilius.users.infrastructure.controllers.routes.UserController.downloadAvatar(user.id.toString).url,
          Map("hash" -> attachment.attachmentId.toString) // Cambiamos la url si cambia el avatar (para evitar la caché)
        )
      }

      // Obtener el cover photo actual desde UserAttachment
      coverPhotoAttachment <- userAttachmentRepository.findCurrentByType(user.id, UserAttachmentType.COVER_PHOTO)
      coverPhotoJs = coverPhotoAttachment.map { attachment =>
        UrlUtil.addQueryParameters(
          dev.pompilius.users.infrastructure.controllers.routes.UserController.downloadCoverPhoto(user.id.toString).url,
          Map("hash" -> attachment.attachmentId.toString)
        )
      }

      roles<- userRoleRepository.getAllByUserId(user.id).map(_.map(_.role))

    } yield {
      // Construimos el JSON final en una variable antes del yield
      val finalJson = Json.obj(
        List(
          toJsValueWrapper(Strings.username, user.username),
          toJsValueWrapper(Strings.avatar, avatarJs),
          toJsValueWrapper(Strings.coverPhoto,coverPhotoJs),
          toJsValueWrapper(Strings.interests,user.interests),// se usa aquí usamos el avatar resuelto
          toJsValueWrapper(Strings.firstName, user.firstName),
          toJsValueWrapper(Strings.lastName, user.lastName),
          toJsValueWrapper(Strings.country, countryJson), // usamos aquí country resuelto
          toJsValueWrapper(Strings.bio, user.bio),
          toJsValueWrapper(Strings.created, user.created),
          toJsValueWrapper(Strings.role, roles)
        ).flatten: _*
      )

      finalJson
    }
  }
}
