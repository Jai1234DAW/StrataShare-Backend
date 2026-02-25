package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy
import controllers.Default

//Mirar esto
import dev.pompilius.account.domain.AccountId
import dev.pompilius.country.domain.Country
import dev.dev.pompilius.mail.domain.MailAddress
import dev.pompilius.shared.infrastructure.PlayConfiguration
import org.joda.time.DateTime
import play.api.i18n.Lang

import java.nio.file.Path
import javax.crypto.spec.SecretKeySpec
import scala.collection.immutable.HashSet
import scala.concurrent.duration.{Duration, FiniteDuration}

@ImplementedBy(classOf[PlayConfiguration])
trait Configuration {

  def environment: String
  def nodeId: Int
  def nodes: List[Int]
  def isTheLocalEnv: Boolean
  def isTheDefaultNode: Boolean
  def logAllExceptions: Boolean

  def baseUrl: String
  def useSSL: Boolean

  //App
  case class App(
      name: String,
      version: String,
      scalaVersion: String,
      sbtVersion: String,
      buildTime: DateTime,
      //Mirar esto para que se utiliza.
      gitBranch: String,
      gitCommit: String,
      startTime: DateTime
  )

  def app:App

  //Heartbeat
  case class Heartbeat(
      initialDelay: FiniteDuration,
      interval: FiniteDuration
                      )
  def heartbeat:Heartbeat


  //Default
  case class Default(
      timeZone: String,
      lang: Lang
                    )
  def default: Default

  //Parámetros globales de configuración.
  case class Context(
      parameters: Map[String, String]
                    )

  def context: Context

  case class Session(
                    maxAge: FiniteDuration
                    )

  // Auth
  case class Auth(
                   resetLinkDuration: FiniteDuration,
                   resetPasswordUrl: String,
                   maxRequest: Int,
                   timeWindow: FiniteDuration
                 )

  def auth: Auth

  // Cache
  case class Cache(
                    enabled: Boolean,
                    duration: FiniteDuration
                  )

  def cache: Cache

  // Users
  case class Users(
                    validateMailLinkDuration: FiniteDuration,
                    validateMailUrl: String,
                    changeMailLinkDuration: FiniteDuration,
                    changeMailUrl: String
                  )

  def users: Users

  // Countries
  def countries: Countries

  case class Countries(
                        featured: List[Country],
                        allowCountryOverride: Boolean
                      )

  // Exports, esto probablemente se quite. Mirar esto.
  case class Exports(
                      maxRowCount: Int
                    )

  def exports: Exports

  // EagerSingletons
  def eagerSingletonsEnabled: Seq[String]

  // RateLimit
  def rateLimit: RateLimit

  case class RateLimit(
                        maxRequest: Int,
                        timeWindow: FiniteDuration,
                        whitelist: HashSet[String]
                      )

  // Task Scheduler
  def taskScheduler: TaskScheduler

  case class TaskScheduler(
                            enabled: Boolean,
                            initialDelay: FiniteDuration,
                            pollingInterval: FiniteDuration,
                            lockTimeout: FiniteDuration,
                            minInterval: FiniteDuration,
                            maxRetryCount: Int
                          )

  // Slack. Mirar esto probablemente se quite
  case class Slack(
                    defaultUrl: Option[String],
                    channels: Map[String, String]
                  )

  def slack: Slack

  def sendgrid: Sendgrid

  case class Sendgrid(
                       enabled: Boolean,
                       apiKey: String,
                       baseUrl: String,
                       from: MailAddress,
                       replyTo: MailAddress,
                       timeout: Duration
                     )

  def sendGridByAccount: Map[AccountId, Sendgrid]

  def getSendGridByAccount(accountId: AccountId): Sendgrid = {
    sendGridByAccount.getOrElse(accountId, sendgrid)
  }

  def mails: Mails

  case class Mails(
                    whitelistDomains: HashSet[String],
                    disposableDomains: HashSet[String],
                    allowDisposableMails: Boolean,
                    allowAlias: Boolean,
                    tokenSecretKey: SecretKeySpec,
                    sendEmailQueueInitialDelay: FiniteDuration,
                    sendEmailQueueInterval: FiniteDuration
                  )

  // Cloudflare. MIRAR ESTO esto probablemente no lo usaré.
  case class CloudflareImages(
                               timeout: Duration,
                               baseUrl: String,
                               accountId: String,
                               apiToken: String,
                               deliveryUrl: String,
                               variants: List[String],
                               defaultVariant: String
                             )

  def cloudflareImages: CloudflareImages

  /*case class Secrets(
                      expirationTime: FiniteDuration,
                      destroyTime: FiniteDuration,
                      cleanerInterval: FiniteDuration,
                      cleanerInitialDelay: FiniteDuration,
                      cleanerBatchSize: Int
                    )

  def secrets: Secrets

  case class SharedFiles(
                          path: Path,
                          expirationTime: FiniteDuration,
                          destroyTime: FiniteDuration,
                          cleanerInterval: FiniteDuration,
                          cleanerInitialDelay: FiniteDuration,
                          cleanerBatchSize: Int
                        )

  def sharedFiles: SharedFiles

}*/

}