package dev.pompilius.shared.domain

import com.google.inject.ImplementedBy

//import javax.crypto.spec.SecretKeySpec

//Mirar esto
import dev.pompilius.shared.infrastructure.PlayConfiguration
import org.joda.time.DateTime
import play.api.i18n.Lang

import scala.collection.immutable.HashSet
import scala.concurrent.duration.FiniteDuration

@ImplementedBy(classOf[PlayConfiguration])
trait Configuration {

  def environment: String
  def isTheLocalEnv: Boolean
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
      //Mirar esto para qué se utiliza.
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

  case class Session(
      maxAge: FiniteDuration
  )

  def session: Session

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

  // EagerSingletons
  def eagerSingletonsEnabled: Seq[String]

  // RateLimit
  def rateLimit: RateLimit

  case class RateLimit(
      maxRequest: Int,
      timeWindow: FiniteDuration,
      whitelist: HashSet[String]
  )

  //def mails: Mails

  //case class Mails(
      //whitelistDomains: HashSet[String],
      //disposableDomains: HashSet[String],
      //allowDisposableMails: Boolean,
      //allowAlias: Boolean,
      //tokenSecretKey: SecretKeySpec,
      //sendEmailQueueInitialDelay: FiniteDuration,
      //sendEmailQueueInterval: FiniteDuration
  //)

}
