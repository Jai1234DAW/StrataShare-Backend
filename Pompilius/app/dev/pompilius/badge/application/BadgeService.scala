package dev.pompilius.badge.application

import com.google.inject.ImplementedBy
import dev.pompilius.badge.domain._
import dev.pompilius.badge.domain.exceptions.BadgeNotFoundException
import dev.pompilius.event.domain.{EventU, UserEvent, UserEventId, UserEventRepository}
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.users.domain.UserId
import org.apache.pekko.Done
import play.api.Logger

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[BadgeServiceImpl])
trait BadgeService {

  //Registra un evento para un usuario y verifica si debe otorgar badges
  def registerEventAndCheckBadges(userId: UserId, eventType: EventU): Future[List[Badge]]

  // Obtiene todos los badges de un usuario
  def getUserBadges(userId: UserId): Future[List[Badge]]

  //Inicializa los badges del sistema (debe llamarse al inicio de la app)
  def initializeSystemBadges(): Future[Done]
}

@Singleton
class BadgeServiceImpl @Inject() (
    userEventRepository: UserEventRepository,
    badgeRepository: BadgeRepository,
    userBadgeRepository: UserBadgeRepository,
    clock: Clock,
    configuration: Configuration
)(implicit ec: ExecutionContext)
    extends BadgeService {

  private val logger = Logger(this.getClass)

  // Configuración de requisitos para badges (temática geológica)
  private val SEDIMENT_COLLECTOR_THRESHOLD = 5 // Coleccionista de Sedimentos
  private val MINERAL_PROSPECTOR_THRESHOLD = 15 // Prospector de Minerales
  private val CRYSTAL_SEEKER_THRESHOLD = 30 // Buscador de Cristales
  private val DIAMOND_EXPLORER_THRESHOLD = 50 // Explorador de Diamantes

  private val FOSSIL_TRADER_THRESHOLD = 3 // Comerciante de Fósiles
  private val ROCK_EXCHANGER_THRESHOLD = 10 // Intercambiador de Rocas
  private val GEMSTONE_SWAPPER_THRESHOLD = 20 // Permutador de Gemas
  private val GEODE_MASTER_THRESHOLD = 40 // Maestro de Geodas

  private val STRATA_CONTRIBUTOR_THRESHOLD = 10 // Contribuidor de Estratos
  private val GEOLOGICAL_LEGEND_THRESHOLD = 50 // Leyenda Geológica

  override def registerEventAndCheckBadges(userId: UserId, eventType: EventU): Future[List[Badge]] = {
    val userEvent = UserEvent(
      id = UserEventId.gen(configuration.nodeId),
      userId = userId,
      event = eventType,
      created = clock.now
    )

    for {
      // 1. Registrar el evento
      _ <- userEventRepository.save(userEvent)

      // 2. Verificar y otorgar badges según el tipo de evento
      earnedBadges <- eventType match {
        case EventU.PURCHASE_COMPLETED =>
          checkAndAwardPurchaseBadges(userId)
        case EventU.BARTER_COMPLETED =>
          checkAndAwardBarterBadges(userId)
        case EventU.SAMPLE_UPLOADED | EventU.STUDY_UPLOADED =>
          checkAndAwardContributorBadges(userId)
        case _ =>
          Future.successful(List.empty[Badge])
      }

      _ = if (earnedBadges.nonEmpty) {
        logger.info(
          s"User ${userId.id} earned ${earnedBadges.length} badge(s): ${earnedBadges.map(_.name).mkString(", ")}"
        )
      }

    } yield earnedBadges
  }

  private def checkAndAwardPurchaseBadges(userId: UserId): Future[List[Badge]] = {
    for {
      purchaseCount <- userEventRepository.countByUserAndEvent(userId, EventU.PURCHASE_COMPLETED)

      badgesToAward <- {
        if (purchaseCount >= DIAMOND_EXPLORER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.DIAMOND_EXPLORER)
        } else if (purchaseCount >= CRYSTAL_SEEKER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.CRYSTAL_SEEKER)
        } else if (purchaseCount >= MINERAL_PROSPECTOR_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.MINERAL_PROSPECTOR)
        } else if (purchaseCount >= SEDIMENT_COLLECTOR_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.SEDIMENT_COLLECTOR)
        } else {
          Future.successful(List.empty[Badge])
        }
      }
    } yield badgesToAward
  }

  private def checkAndAwardBarterBadges(userId: UserId): Future[List[Badge]] = {
    for {
      barterCount <- userEventRepository.countByUserAndEvent(userId, EventU.BARTER_COMPLETED)

      badgesToAward <- {
        if (barterCount >= GEODE_MASTER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.GEODE_MASTER)
        } else if (barterCount >= GEMSTONE_SWAPPER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.GEMSTONE_SWAPPER)
        } else if (barterCount >= ROCK_EXCHANGER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.ROCK_EXCHANGER)
        } else if (barterCount >= FOSSIL_TRADER_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.FOSSIL_TRADER)
        } else {
          Future.successful(List.empty[Badge])
        }
      }
    } yield badgesToAward
  }

  private def checkAndAwardContributorBadges(userId: UserId): Future[List[Badge]] = {
    for {
      totalContributions <- userEventRepository.countAllEventsByUserId(userId)

      badgesToAward <- {
        if (totalContributions >= GEOLOGICAL_LEGEND_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.GEOLOGICAL_LEGEND)
        } else if (totalContributions >= STRATA_CONTRIBUTOR_THRESHOLD) {
          awardBadgeIfNotEarned(userId, BadgeType.STRATA_CONTRIBUTOR)
        } else {
          Future.successful(List.empty[Badge])
        }
      }
    } yield badgesToAward
  }

  private def awardBadgeIfNotEarned(userId: UserId, badgeType: BadgeType): Future[List[Badge]] = {
    for {
      alreadyEarned <- userBadgeRepository.hasUserEarnedBadge(userId, badgeType)

      result <-
        if (!alreadyEarned) {
          for {
            badgeOpt <- badgeRepository.findByType(badgeType)
            badge = badgeOpt.getOrElse {
              logger.warn(s"Badge type $badgeType not found in database")
              throw new BadgeNotFoundException(s"Badge type $badgeType is not configured")
            }

            // PK compuesta: (userId, badgeId) - NO necesitamos un ID separado
            userBadge = UserBadge(
              userId = userId,
              badgeId = badge.id,
              earnedAt = clock.now
            )

            _ <- userBadgeRepository.save(userBadge)
            _ = logger.info(s"Awarded badge '${badge.name}' to user ${userId.id}")
          } yield List(badge)
        } else {
          Future.successful(List.empty[Badge])
        }
    } yield result
  }

  override def getUserBadges(userId: UserId): Future[List[Badge]] = {
    for {
      userBadges <- userBadgeRepository.findAllByUserId(userId)
      badges <- Future.sequence(
        userBadges.map(ub =>
          badgeRepository
            .findById(ub.badgeId)
            .map(_.getOrElse(throw new BadgeNotFoundException(s"Badge ${ub.badgeId} not found")))
        )
      )
    } yield badges
  }

  override def initializeSystemBadges(): Future[Done] = {
    logger.info("Initializing system badges...")

    val badges = List(
      // Purchase badges (Colección de muestras geológicas)
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.SEDIMENT_COLLECTOR,
        "Coleccionista de Sedimentos",
        s"Adquirió $SEDIMENT_COLLECTOR_THRESHOLD estudios o muestras geológicas",
        Some("/images/badges/sediment_collector.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.MINERAL_PROSPECTOR,
        "Prospector de Minerales",
        s"Adquirió $MINERAL_PROSPECTOR_THRESHOLD estudios o muestras geológicas",
        Some("/images/badges/mineral_prospector.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.CRYSTAL_SEEKER,
        "Buscador de Cristales",
        s"Adquirió $CRYSTAL_SEEKER_THRESHOLD estudios o muestras geológicas",
        Some("/images/badges/crystal_seeker.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.DIAMOND_EXPLORER,
        "Explorador de Diamantes",
        s"Adquirió $DIAMOND_EXPLORER_THRESHOLD estudios o muestras geológicas",
        Some("/images/badges/diamond_explorer.png"),
        clock.now
      ),
      // Barter badges (Intercambio de recursos geológicos)
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.FOSSIL_TRADER,
        "Comerciante de Fósiles",
        s"Completó $FOSSIL_TRADER_THRESHOLD trueques de recursos geológicos",
        Some("/images/badges/fossil_trader.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.ROCK_EXCHANGER,
        "Intercambiador de Rocas",
        s"Completó $ROCK_EXCHANGER_THRESHOLD trueques de recursos geológicos",
        Some("/images/badges/rock_exchanger.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEMSTONE_SWAPPER,
        "Permutador de Gemas",
        s"Completó $GEMSTONE_SWAPPER_THRESHOLD trueques de recursos geológicos",
        Some("/images/badges/gemstone_swapper.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEODE_MASTER,
        "Maestro de Geodas",
        s"Completó $GEODE_MASTER_THRESHOLD trueques de recursos geológicos",
        Some("/images/badges/geode_master.png"),
        clock.now
      ),
      // Contributor badges (Participación en la comunidad geológica)
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.STRATA_CONTRIBUTOR,
        "Contribuidor de Estratos",
        s"Participó activamente en $STRATA_CONTRIBUTOR_THRESHOLD proporcionar documentación geológica",
        Some("/images/badges/strata_contributor.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEOLOGICAL_LEGEND,
        "Leyenda Geológica",
        s"Participó activamente en $GEOLOGICAL_LEGEND_THRESHOLD en proporcionar documentación geológica",
        Some("/images/badges/geological_legend.png"),
        clock.now
      )
    )

    for {
      _ <- Future.sequence(badges.map(badge => badgeRepository.save(badge)))
      _ = logger.info(s"Successfully initialized ${badges.length} system badges")
    } yield Done
  }
}
