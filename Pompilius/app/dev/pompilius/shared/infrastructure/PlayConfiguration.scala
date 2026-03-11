package dev.pompilius.shared.infrastructure

import com.typesafe.config.ConfigException.WrongType
import dev.pompilius.Strings
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.BuildInfo
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Environment
import play.api.i18n.Lang

import javax.inject.{Inject, Singleton}
import scala.collection.immutable.HashSet
import scala.concurrent.duration.FiniteDuration

@SuppressWarnings(Array("UnusedMethodParameter"))
@Singleton
class PlayConfiguration @Inject() (
  playConfig: play.api.Configuration,
  env: Environment,
  clock: Clock
) extends Configuration {

  private val logger = play.api.Logger(getClass)

  // Root
  override val environment: String = playConfig.get[String](Strings.environment)
  override val isTheLocalEnv: Boolean = {
    environment.equalsIgnoreCase(Strings.local)
  }

  override val logAllExceptions: Boolean = {
    playConfig.get[Boolean](Strings.logAllExceptions)
  }

  override val baseUrl: String = playConfig.get[String](Strings.baseUrl)
  override val useSSL: Boolean = playConfig.get[Boolean](Strings.useSSL)

  // App
  override val app: App = App(
    name = BuildInfo.name,
    version = BuildInfo.version,
    scalaVersion = BuildInfo.scalaVersion,
    sbtVersion = BuildInfo.sbtVersion,
    buildTime = new DateTime(BuildInfo.builtAtMillis),
    gitBranch = BuildInfo.gitBranch,
    gitCommit = BuildInfo.gitCommit,
    startTime = clock.now
  )

  // Default
  override val default: Default = Default(
    timeZone = DateTimeZone.getDefault.toString,
    lang = Lang("es")
  )

  private val contextParameters: Map[String, String] = {
    val conf = playConfig.get[play.api.Configuration]("context.parameters")
    conf.subKeys.flatMap { key =>
      try {
        Some(key -> conf.get[String](key))
      } catch {
        case _: WrongType => None
      }
    }.toMap
  }

  override val context: Context = Context(
    parameters = contextParameters
  )

  // Session
  override val session: Session = Session(
    maxAge = playConfig.get[FiniteDuration]("play.http.session.maxAge")
  )

  // Auth
  override val auth: Auth = Auth(
    maxRequest = playConfig.get[Int]("auth.maxRequest"),
    timeWindow = playConfig.get[FiniteDuration]("auth.timeWindow"),
    resetLinkDuration = playConfig.get[FiniteDuration]("auth.resetLinkDuration"),
    resetPasswordUrl = playConfig.get[String]("auth.resetPasswordUrl"),
    maxAge = playConfig.get[FiniteDuration]("auth.maxAge"),
  )

  // Cache
  override val cache: Cache = Cache(
    enabled = playConfig.get[Boolean]("cache.enabled"),
    duration = playConfig.get[FiniteDuration]("cache.duration")
  )

  // Users
  override val users: Users = Users(
    validateMailLinkDuration = playConfig.get[FiniteDuration]("users.validateMailLinkDuration"),
    validateMailUrl = playConfig.get[String]("users.validateMailUrl"),
    changeMailLinkDuration = playConfig.get[FiniteDuration]("users.changeMailLinkDuration"),
    changeMailUrl = playConfig.get[String]("users.changeMailUrl")
  )

  // EagerSingletons
  override val eagerSingletonsEnabled: Seq[String] =
    playConfig
      .get[Seq[String]]("eagerSingletons.enabled")
      .distinct
      .diff(
        playConfig.get[Seq[String]]("eagerSingletons.disabled").distinct
      )

  override def rateLimit: RateLimit =
    RateLimit(
      maxRequest = playConfig.get[Int]("rateLimit.maxRequest"),
      timeWindow = playConfig.get[FiniteDuration]("rateLimit.timeWindow")
    )

}
