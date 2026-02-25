package dev.pompilius.shared.infrastructure

import com.typesafe.config.ConfigException.WrongType
import dev.pompilius.account.domain.AccountId
import dev.pompilius.country.domain.Country
import dev.pompilius.mail.domain.MailAddress
import dev.pompilius.shared.domain.{Clock, Configuration}
import dev.pompilius.{BuildInfo, Strings}
import dev.pompilius.Strings
import org.joda.time.{DateTime, DateTimeZone}
import play.api.i18n.Lang
import play.api.libs.json.Json
import play.api.{ConfigLoader, Environment}

import java.net.URI
import java.nio.file.Paths
import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.inject.{Inject, Singleton}
import scala.collection.immutable.HashSet
import scala.concurrent.duration.{Duration, FiniteDuration}
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
  override val nodes: List[Int] = playConfig.get[Seq[Int]](Strings.nodes).toList
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
//   override val app: App = App(
//     name = BuildInfo.name,
//     version = BuildInfo.version,
//     scalaVersion = BuildInfo.scalaVersion,
//     sbtVersion = BuildInfo.sbtVersion,
//     buildTime = new DateTime(BuildInfo.builtAtMillis),
//     gitBranch = BuildInfo.gitBranch,
//     gitCommit = BuildInfo.gitCommit,
//     startTime = clock.now
//   )

  // Heartbeat
  override val heartbeat: Heartbeat = Heartbeat(
    initialDelay = playConfig.get[FiniteDuration]("heartbeat.initialDelay"),
    interval = playConfig.get[FiniteDuration]("heartbeat.interval")
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

  override val countries: Countries = Countries(
    featured = playConfig.get[Seq[String]]("countries.featured").flatMap(Country.withNameInsensitiveOption).toList,
    allowCountryOverride = playConfig.get[Boolean]("countries.allowCountryOverride")
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
    resetPasswordUrl = playConfig.get[String]("auth.resetPasswordUrl")
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

  // Exports
  override val exports: Exports = Exports(
    maxRowCount = playConfig.get[Int]("exports.maxRowCount")
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
      timeWindow = playConfig.get[FiniteDuration]("rateLimit.timeWindow"),
      whitelist = HashSet(playConfig.get[Seq[String]]("rateLimit.whitelist"): _*)
    )

  // Task Scheduler
  override def taskScheduler: TaskScheduler =
    TaskScheduler(
      enabled = playConfig.get[Boolean]("taskScheduler.enabled"),
      initialDelay = playConfig.get[FiniteDuration]("taskScheduler.initialDelay"),
      pollingInterval = playConfig.get[FiniteDuration]("taskScheduler.pollingInterval"),
      lockTimeout = playConfig.get[FiniteDuration]("taskScheduler.lockTimeout"),
      minInterval = playConfig.get[FiniteDuration]("taskScheduler.minInterval"),
      maxRetryCount = playConfig.get[Int]("taskScheduler.maxRetryCount")
    )

  // Slack
  override val slack: Slack = Slack(
    defaultUrl = playConfig.getOptional[String]("slack.defaultUrl"),
    channels = Json.parse(playConfig.getOptional[String]("slack.channels").getOrElse("{}")).as[Map[String, String]]
  )

  // SendGrid
  override val sendgrid: Sendgrid = Sendgrid(
    enabled = playConfig.get[Boolean]("sendgrid.enabled"),
    apiKey = playConfig.get[String]("sendgrid.apiKey"),
    baseUrl = playConfig.get[String]("sendgrid.baseUrl"),
    from = playConfig.get[MailAddress]("sendgrid.from"),
    replyTo = playConfig.get[MailAddress]("sendgrid.replyTo"),
    timeout = playConfig.get[FiniteDuration]("sendgrid.timeout")
  )

  override val sendGridByAccount: Map[AccountId, Sendgrid] = {
    playConfig.get[Map[String, play.api.Configuration]]("sendgrid.byAccount").map {
      case (accountId, config) =>
        AccountId(accountId) -> Sendgrid(
          enabled = playConfig.get[Boolean]("sendgrid.enabled"),
          apiKey = config.get[String]("apiKey"),
          baseUrl = playConfig.get[String]("sendgrid.baseUrl"),
          from = config.get[MailAddress]("from"),
          replyTo = config.get[MailAddress]("replyTo"),
          timeout = playConfig.get[FiniteDuration]("sendgrid.timeout")
        )
    }
  }

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

    val whitelistDomains = playConfig.get[Seq[String]]("mails.domains.whitelist")

    Mails(
      whitelistDomains = HashSet(whitelistDomains: _*),
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

  // Cloudflare
  override val cloudflareImages: CloudflareImages = CloudflareImages(
    timeout = playConfig.get[Duration]("cloudflare.timeout"),
    baseUrl = playConfig.get[String]("cloudflare.images.baseUrl"),
    accountId = playConfig.get[String]("cloudflare.images.accountId"),
    apiToken = playConfig.get[String]("cloudflare.images.apiToken"),
    deliveryUrl = playConfig.get[String]("cloudflare.images.deliveryUrl"),
    variants = playConfig.get[Seq[String]]("cloudflare.images.variants").toList,
    defaultVariant = playConfig.get[String]("cloudflare.images.defaultVariant")
  )

  // Secrets
  override val secrets: Secrets = Secrets(
    expirationTime = playConfig.get[FiniteDuration]("secrets.expirationTime"),
    destroyTime = playConfig.get[FiniteDuration]("secrets.destroyTime"),
    cleanerInterval = playConfig.get[FiniteDuration]("secrets.cleanerInterval"),
    cleanerInitialDelay = playConfig.get[FiniteDuration]("secrets.cleanerInitialDelay"),
    cleanerBatchSize = playConfig.get[Int]("secrets.cleanerBatchSize")
  )

  // SharedFiles
  override val sharedFiles: SharedFiles = SharedFiles(
    path = Paths.get(playConfig.get[String]("sharedFiles.path")),
    expirationTime = playConfig.get[FiniteDuration]("sharedFiles.expirationTime"),
    destroyTime = playConfig.get[FiniteDuration]("sharedFiles.destroyTime"),
    cleanerInterval = playConfig.get[FiniteDuration]("sharedFiles.cleanerInterval"),
    cleanerInitialDelay = playConfig.get[FiniteDuration]("sharedFiles.cleanerInitialDelay"),
    cleanerBatchSize = playConfig.get[Int]("sharedFiles.cleanerBatchSize")
  )

}
