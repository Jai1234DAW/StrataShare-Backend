package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy
import dev.pompilius.country.domain.Country
import dev.pompilius.mail.domain.MailAddress

import java.nio.file.Path
import javax.crypto.spec.SecretKeySpec
import dev.pompilius.shared.infrastructure.PlayConfiguration
import org.joda.time.DateTime
import play.api.i18n.Lang

import scala.collection.immutable.HashSet
import scala.concurrent.duration.{Duration, FiniteDuration}

@ImplementedBy(classOf[PlayConfiguration])
trait Configuration {

  def environment: String
  def nodeId: Int
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
      gitBranch: String,
      gitCommit: String,
      startTime: DateTime
  )

  def app: App

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

  def countries: Countries

  case class Countries(
      featured: List[Country],
      allowCountryOverride: Boolean
  )

  case class Session(
      maxAge: FiniteDuration
  )

  def session: Session

  // Auth
  case class Auth(
      resetLinkDuration: FiniteDuration,
      resetPasswordUrl: String,
      maxRequest: Int,
      timeWindow: FiniteDuration,
      maxAge: FiniteDuration,
      allowLoginWithoutRoles: Boolean
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

  // EagerSingletons
  def eagerSingletonsEnabled: Seq[String]

  // RateLimit
  def rateLimit: RateLimit

  case class RateLimit(
      maxRequest: Int,
      timeWindow: FiniteDuration
  )

  // Attachments
  case class Avatars(
      maxWidth: Int,
      maxHeight: Int
  )
  case class Attachments(
      path: Path,
      avatars: Avatars
  )

  def attachments: Attachments

  //MIRAR ESTA CONFIGURACIÓN AQUÍ
  def mails: Mails

  case class Mails(
      disposableDomains: HashSet[String],
      allowDisposableMails: Boolean,
      allowAlias: Boolean,
      tokenSecretKey: SecretKeySpec,
      sendEmailQueueInitialDelay: FiniteDuration,
      sendEmailQueueInterval: FiniteDuration
  )
  case class Mail(
      host: String,
      username: Option[String],
      password: Option[String],
      smtpPort: Option[Int],
      sslSmtpPort: Option[Int],
      setSslOnConnect: Option[Boolean],
      sslCheckServerIdentity: Option[Boolean],
      startTlsEnabled: Option[Boolean],
      startTlsRequired: Option[Boolean],
      from: MailAddress,
      replyTo: Option[MailAddress]
  )

  def mail: Mail

  // Barter
  case class Barter(
      requestLinkDuration: FiniteDuration,
      purchaseResourceUrl: String
  )

  def barter: Barter

  // Stripe
  case class Stripe(
      sandbox: Boolean,
      secretKey: String,
      publishableKey: String,
      webhookSecret: String,
      apiUrl: String,
      currency: String
  )

  def stripe: Stripe

  case class Payments(
      currency: String,
      //allowedOneTimePaymentGateways: List[AllowedGateway],
      paymentCompletedUrl: String,
      paymentCanceledUrl: String,
      defaultFee: BigDecimal,
      feeOwnPlatform: BigDecimal
  )

  def payments: Payments

  case class GatewaySurcharges(
      stripeSurchargePercentage: BigDecimal,
      stripeSurchargeFixed: BigDecimal
  )

  def gatewaySurcharges: GatewaySurcharges
}
