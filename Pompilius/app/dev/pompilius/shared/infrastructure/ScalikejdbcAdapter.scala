package dev.pompilius.shared.infrastructure

import com.typesafe.config.Config
import play.api.Configuration
import play.api.db.DBApi
import scalikejdbc.config.{DBs, TypesafeConfig, TypesafeConfigReader}
import scalikejdbc.{DataSourceConnectionPool, GlobalSettings}

import javax.inject.{Inject, Singleton}

@Singleton
class ScalikejdbcAdapter @Inject() (
    dbApi: DBApi,
    configuration: Configuration
) {

  private[this] lazy val DBs = new DBs with TypesafeConfigReader with TypesafeConfig {
    override val config: Config = configuration.underlying
  }

  private[this] lazy val loggingSQLErrors =
    configuration.getOptional[Boolean]("scalikejdbc.global.loggingSQLErrors").getOrElse(true)

  private def onStart(): Unit = {
    DBs.loadGlobalSettings()
    GlobalSettings.loggingSQLErrors = loggingSQLErrors

    dbApi.databases().foreach { db =>
      scalikejdbc.ConnectionPool.add(db.name, new DataSourceConnectionPool(db.dataSource))
    }
  }
  onStart()
}
