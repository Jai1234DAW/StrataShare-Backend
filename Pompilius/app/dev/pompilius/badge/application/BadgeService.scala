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
        "Sediment Collector",
        s"Acquired $SEDIMENT_COLLECTOR_THRESHOLD samples or geological studies",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778874624/SEDIMENT_COLLECTOR_txdplm.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.MINERAL_PROSPECTOR,
        "Mineral Prospector",
        s"Acquired $MINERAL_PROSPECTOR_THRESHOLD studies or geological samples",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778875277/MINERAL_PROSPECTOR_q2m20g.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.CRYSTAL_SEEKER,
        "Crystal Seeker",
        s"Acquired $CRYSTAL_SEEKER_THRESHOLD samples or geological studies",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778875115/CRYSTAL_SEEKER_yhfmrt.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.DIAMOND_EXPLORER,
        "Diamond Explorer",
        s"Acquired $DIAMOND_EXPLORER_THRESHOLD samples or geological studies",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778875462/DIAMOND_EXPLORER_tjmol6.png"),
        clock.now
      ),
      // Barter badges (Intercambio de recursos geológicos)
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.FOSSIL_TRADER,
        "Fossil Trader",
        s"Completed $FOSSIL_TRADER_THRESHOLD geological resources barters",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778876128/FOSSIL_TRADER_frfnrz.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.ROCK_EXCHANGER,
        "Rock Exchanger",
        s"Completed $ROCK_EXCHANGER_THRESHOLD geological resources barters",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778877446/ROCK_EXCHANGER_ewk2hd.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEMSTONE_SWAPPER,
        "Gemstone Swapper",
        s"Completed $GEMSTONE_SWAPPER_THRESHOLD geological resources barters",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778877499/GEMSTONE_SWAPPER_nrtmox.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEODE_MASTER,
        "Geode Master",
        s"Completed $GEODE_MASTER_THRESHOLD geological resources barters",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778878878/GEODE_MASTER_quh4xm.png"),
        clock.now
      ),
      // Contributor badges (Participación en la comunidad geológica)
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.STRATA_CONTRIBUTOR,
        "Strata Contributor",
        s"Actively participated in contributing for $STRATA_CONTRIBUTOR_THRESHOLD geological documentation",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778878841/STRATA_CONTRIBUTOR_yfoope.png"),
        clock.now
      ),
      Badge(
        BadgeId.gen(configuration.nodeId),
        BadgeType.GEOLOGICAL_LEGEND,
        "Geological Legend",
        s"Actively participated in contributing for $GEOLOGICAL_LEGEND_THRESHOLD geological documentation",
        Some("https://res.cloudinary.com/dk7yydz0v/image/upload/v1778878470/GEOLOGICAL_LEGEND_hc1o96.png"),
        clock.now
      )
    )

    for {
      _ <- Future.sequence(badges.map(badge => badgeRepository.save(badge)))
      _ = logger.info(s"Successfully initialized ${badges.length} system badges")
    } yield Done
  }
}
