package dev.pompilius.badge.application

import dev.pompilius.badge.domain.BadgeRepository
import dev.pompilius.shared.domain.{Clock, Configuration}
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Inicializador que se ejecuta al arranque de la aplicación
 * para inicializar los badges del sistema
 */
@Singleton
class BadgeInitializer @Inject() (
    badgeService: BadgeService,
    badgeRepository: BadgeRepository,
    clock: Clock,
    configuration: Configuration
)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)

  // Se ejecuta automáticamente al inyectar esta clase
  initialize()

  private def initialize(): Unit = {
    logger.info("Starting badge system initialization...")

    badgeRepository.findAll.flatMap { existingBadges =>
      if (existingBadges.isEmpty) {
        logger.info("No badges found in database. Initializing system badges...")
        badgeService.initializeSystemBadges().map { _ =>
          logger.info("Badge system initialized successfully")
        }.recover {
          case e: Exception =>
            logger.error(s"Failed to initialize badge system: ${e.getMessage}", e)
        }
      } else {
        logger.info(s"Badge system already initialized with ${existingBadges.length} badges")
        Future.successful(())
      }
    }.recover {
      case e: Exception =>
        logger.error(s"Error checking badge system: ${e.getMessage}", e)
    }
  }
}

