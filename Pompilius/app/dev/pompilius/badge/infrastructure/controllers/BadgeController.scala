package dev.pompilius.badge.infrastructure.controllers

import dev.pompilius.attachment.infrastructure.Attachments
import dev.pompilius.badge.application.BadgeService
import dev.pompilius.badge.domain.exceptions.BadgeNotFoundException
import dev.pompilius.badge.domain.{BadgeRepository, BadgeType}
import dev.pompilius.badge.infrastructure.parsers.UpdateBadgeImageRequestParser
import dev.pompilius.badge.infrastructure.writers.BadgeWriter
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.{Role, UserId}
import play.api.Logger
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class BadgeController @Inject() (
    badgeService: BadgeService,
    badgeRepository: BadgeRepository,
    badgeWriter: BadgeWriter,
)(implicit val ec: ExecutionContext)
    extends BaseController with Attachments {

  private val logger = Logger(this.getClass)

  // Obtiene todos los badges que ha ganado el usuario autenticado
  def myBadges: Action[AnyContent] =
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          for {
            badges <- badgeService.getUserBadges(user.id)
            json <- badgeWriter.asList(badges)
          } yield Ok(json)
      }
    }

  //Lista todos los badges disponibles en el sistema (solo para admins)
  def allBadges: Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, _, _, _) =>
          for {
            allBadges <- badgeRepository.findAll
            json <- badgeWriter.asList(allBadges)
          } yield Ok(json)
      }
    }

  /**
    * Actualiza la URL de la imagen de un badge específico (solo para user admins)
    * Body: { "imageUrl": "https://cdn.example.com/badges/sediment.png" }
    */

  def updateBadgeImage(badgeTypeStr: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, _, _, _) =>
          val updateRequest = UpdateBadgeImageRequestParser.parse(request)

          // Validar que el badgeType existe
          val badgeType = BadgeType
            .withNameInsensitiveOption(badgeTypeStr)
            .getOrElse(throw new BadRequestException(s"Invalid badge type: $badgeTypeStr"))

          for {
            // Buscar el badge
            badgeOpt <- badgeRepository.findByType(badgeType)
            badge = badgeOpt.getOrElse(
              throw new BadgeNotFoundException(s"Badge not found for type: $badgeTypeStr")
            )

            // Actualizar con la nueva imageUrl
            updatedBadge = badge.copy(imageUrl = Some(updateRequest.imageUrl))
            _ <- badgeRepository.save(updatedBadge)

            // Retornar el badge actualizado
            json <- badgeWriter.toJson(updatedBadge)
            _ = logger.info(s"Updated image URL for badge type: $badgeTypeStr to ${updateRequest.imageUrl}")

          } yield Ok(json)
      }
    }

  //Funcionalidades futuras a implementar: subir una imagen para un badge (solo para admins) y descargar la imagen de un badge
//  def uploadBadgeImage(badgeTypeStr: String): Action[MultipartFormData[Files.TemporaryFile]] =
//    Action.async(parse.multipartFormData) { implicit request =>
//      withAnyOfThisRoles(Seq(Role.ADMIN)) {
//        case (_, user, _, _) =>
//          val badgeType = BadgeType
//            .withNameInsensitiveOption(badgeTypeStr)
//            .getOrElse(throw new BadRequestException(s"Invalid badge type: $badgeTypeStr"))
//
//          for {
//            // Cargar la imagen
//            attachment <- uploadImage(
//              user = user,
//              body = request.body,
//              maxWidth = configuration.attachments.avatars.maxWidth,
//              maxHeight = configuration.attachments.avatars.maxHeight
//            )
//
//            // Obtener el badge
//            badgeOpt <- badgeRepository.findByType(badgeType)
//            badge = badgeOpt.getOrElse(
//              throw new BadRequestException(s"Badge not found for type: $badgeTypeStr")
//            )
//
            // Actualizar badge con la imagen
//            updatedBadge = badge.copy(
   //           image = Some(attachment.id),
//              updated = clock.now
//            )
//
//            // Guardar el badge actualizado
//            _ <- badgeRepository.save(updatedBadge)
//
//            // Retornar el badge actualizado
//            json <- badgeWriter.toJson(updatedBadge)
//            _ = logger.info(s"Uploaded image for badge type: $badgeTypeStr, attachment id: ${attachment.id}")
//
//          } yield {
//            Ok(json)
//          }
//      }
//    }
//
//
////  def downloadBadgeImage(badgeTypeStr: String): Action[AnyContent] =
////    Action.async { implicit request =>
////      withAuthenticatedUser {
////        case (_, user, _) =>
//          val pid = user.id
////          for {
////
//            badge <- badgeRepository.findByType(
//              BadgeType.withNameInsensitiveOption(badgeTypeStr).getOrElse(
//                throw new BadRequestException(s"Invalid badge type: $badgeTypeStr")
//              )
//            ).map(_.getOrElse(throw new BadRequestException(s"Badge not found for type: $badgeTypeStr")))
////
//            attachmentId = badge.image.getOrElse(throw new AttachmentNotFoundException(s"Badge with id $badgeTypeStr has no associated picture"))
//
//            result <- download(Some(user), attachmentId)
////
////          } yield {
////            result
////          }
////      }
////    }


  //Elimina la imagen de un badge - solo para admins
  def removeBadgeImage(badgeTypeStr: String): Action[AnyContent] = {
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, _, _, _) =>
          val badgeType = BadgeType
            .withNameInsensitiveOption(badgeTypeStr)
            .getOrElse(throw new BadRequestException(s"Invalid badge type: $badgeTypeStr"))

          for {
            badgeOpt <- badgeRepository.findByType(badgeType)
            badge = badgeOpt.getOrElse(
              throw new BadgeNotFoundException(s"Badge not found for type: $badgeTypeStr")
            )

            updatedBadge = badge.copy(imageUrl = None)
            _ <- badgeRepository.save(updatedBadge)

            json <- badgeWriter.toJson(updatedBadge)
            _ = logger.info(s"Removed image URL for badge type: $badgeTypeStr")

          } yield Ok(json)
      }
    }
  }

  def listAllBadgesFromAnotherUser(userIdStr: String): Action[AnyContent] = {
    Action.async { implicit request =>
      withAuthenticatedUser {
        case (_, user, _) =>
          val userId = UserId(userIdStr)
          for {
            badges <- badgeService.getUserBadges(userId)
            json <- badgeWriter.asList(badges)
          } yield Ok(json)
      }
    }
  }
}
