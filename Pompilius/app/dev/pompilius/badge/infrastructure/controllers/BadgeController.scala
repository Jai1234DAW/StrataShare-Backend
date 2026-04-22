package dev.pompilius.badge.infrastructure.controllers

import dev.pompilius.badge.application.BadgeService
import dev.pompilius.badge.domain.{BadgeRepository, BadgeType}
import dev.pompilius.badge.infrastructure.parsers.UpdateBadgeImageRequestParser
import dev.pompilius.badge.infrastructure.writers.BadgeWriter
import dev.pompilius.shared.domain.exceptions.BadRequestException
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.shared.infrastructure.BaseController
import dev.pompilius.users.domain.Role
import play.api.Logger
import play.api.mvc.{Action, AnyContent}

import javax.inject._
import scala.concurrent.ExecutionContext

@Singleton
class BadgeController @Inject() (
    badgeService: BadgeService,
    badgeRepository: BadgeRepository,
    badgeWriter: BadgeWriter,
    clock: Clock,
    configuration: Configuration
)(implicit val ec: ExecutionContext)
    extends BaseController {

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
    *
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
              throw new BadRequestException(s"Badge not found for type: $badgeTypeStr")
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

  //Elimina la imagen de un badge - solo para admins

  def removeBadgeImage(badgeTypeStr: String): Action[AnyContent] =
    Action.async { implicit request =>
      withAnyOfThisRoles(Seq(Role.ADMIN)) {
        case (_, _, _, _) =>
          val badgeType = BadgeType
            .withNameInsensitiveOption(badgeTypeStr)
            .getOrElse(throw new BadRequestException(s"Invalid badge type: $badgeTypeStr"))

          for {
            badgeOpt <- badgeRepository.findByType(badgeType)
            badge = badgeOpt.getOrElse(
              throw new BadRequestException(s"Badge not found for type: $badgeTypeStr")
            )

            updatedBadge = badge.copy(imageUrl = None)
            _ <- badgeRepository.save(updatedBadge)

            json <- badgeWriter.toJson(updatedBadge)
            _ = logger.info(s"Removed image URL for badge type: $badgeTypeStr")

          } yield Ok(json)
      }
    }
}
