package dev.pompilius.shared.infrastructure

import com.typesafe.config.ConfigException.WrongType
import dev.pompilius.Strings
import dev.pompilius.country.domain.Country
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.BuildInfo
import dev.pompilius.mail.domain.MailAddress
import org.joda.time.{DateTime, DateTimeZone}
import play.api.{ConfigLoader, Environment}
import play.api.i18n.Lang

import java.net.URI
import java.nio.file.Paths
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import scala.collection.immutable.HashSet
import scala.concurrent.duration.FiniteDuration
import scala.io.Source
import scala.util.Try
import scala.util.control.NonFatal

@SuppressWarnings(Array("UnusedMethodParameter"))
@Singleton
class PlayConfiguration @Inject() (
    playConfig: play.api.Configuration,
    env: Environment,
    clock: Clock
) extends Configuration {

  private val logger = play.api.Logger(getClass)

  implicit def mailAddressLoader(implicit loader: ConfigLoader[Option[String]]): ConfigLoader[MailAddress] =
    ConfigLoader(_.getConfig).map { config =>
      MailAddress(
        address = config.getString("address"),
        name = loader.load(config, "name")
      )
    }

  // Root
  override val environment: String = playConfig.get[String](Strings.environment)
  override val nodeId: Int = playConfig.get[Int](Strings.nodeId)
  override val isTheLocalEnv: Boolean = {
    environment.equalsIgnoreCase(Strings.local)
  }

  override val isTheDefaultNode: Boolean = {
    nodeId == 0
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
    lang = Lang("en")
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
    allowLoginWithoutRoles = playConfig.get[Boolean]("auth.allowLoginWithoutRoles")
  )

  // Attachments
  override val attachments: Attachments = Attachments(
    path = Paths.get(playConfig.get[String]("attachments.path")),
    avatars = Avatars(
      maxWidth = playConfig.get[Int]("attachments.avatars.maxWidth"),
      maxHeight = playConfig.get[Int]("attachments.avatars.maxHeight")
    )
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

  //Country
  override val countries: Countries = Countries(
    featured = playConfig.get[Seq[String]]("countries.featured").flatMap(Country.withNameInsensitiveOption).toList,
    allowCountryOverride = playConfig.get[Boolean]("countries.allowCountryOverride")
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

  override val mails: Mails = {
    // Carga la lista de dominios de email desechables
    val disposableEmailDomains: HashSet[String] =
      try {
        val path = playConfig.get[String]("mails.domains.blacklist")
        val lines =
          Try(Source.fromFile(path, "UTF-8"))
            .orElse(Try(Source.fromURL(new URI(path).toURL, "UTF-8")))
            .getOrElse {
              logger.warn(s"Unable to load disposable email domains from $path, using default list")
              Source.fromURL(getClass.getResource("/disposable_email_blocklist.conf"), "UTF-8")
            }
            .getLines()
            .map(_.trim.toLowerCase)
            .filterNot(_.startsWith("#"))
            .filter(_.nonEmpty)
        HashSet(lines.toSeq: _*)
      } catch {
        case NonFatal(e) =>
          logger.error("Error loading disposable email domains", e)
          HashSet.empty[String]
      }

    val whitelistDomains: Set[String] =
      playConfig
        .get[Seq[String]]("mails.domains.whitelist")
        .map(_.trim.toLowerCase)
        .filter(_.nonEmpty)
        .toSet

    Mails(
      disposableDomains = disposableEmailDomains -- whitelistDomains,
      allowDisposableMails = playConfig.get[Boolean]("mails.allowDisposableMails"),
      allowAlias = playConfig.get[Boolean]("mails.allowAlias"),
      tokenSecretKey = new SecretKeySpec(
        MessageDigest
          .getInstance("SHA-256")
          .digest(playConfig.get[String]("mails.tokenSecretKey").getBytes("UTF-8")),
        "HmacSHA256"
      ),
      sendEmailQueueInitialDelay = playConfig.get[FiniteDuration]("mails.sendEmailQueue.initialDelay"),
      sendEmailQueueInterval = playConfig.get[FiniteDuration]("mails.sendEmailQueue.interval")
    )
  }

  // Mail
  override val mail: Mail = Mail(
    host = playConfig.get[String]("mail.host"),
    smtpPort = playConfig.getOptional[Int]("mail.smtpPort"),
    sslSmtpPort = playConfig.getOptional[Int]("mail.sslSmtpPort"),
    username = playConfig.getOptional[String]("mail.username"),
    password = playConfig.getOptional[String]("mail.password"),
    setSslOnConnect = playConfig.getOptional[Boolean]("mail.setSslOnConnect"),
    sslCheckServerIdentity = playConfig.getOptional[Boolean]("mail.sslCheckServerIdentity"),
    startTlsEnabled = playConfig.getOptional[Boolean]("mail.startTlsEnabled"),
    startTlsRequired = playConfig.getOptional[Boolean]("mail.startTlsRequired"),
    from = playConfig.get[MailAddress]("mail.from"),
    replyTo = playConfig.getOptional[MailAddress]("mail.replyTo")
  )

  // Barter
  override val barter: Barter = Barter(
    requestLinkDuration = playConfig.get[FiniteDuration]("barter.requestLinkDuration"),
    purchaseResourceUrl = playConfig.get[String]("barter.purchaseResourceUrl")
  )
}
