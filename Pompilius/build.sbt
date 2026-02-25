name := """pompilius"""
organization := "dev.pompilius"
version := "1.1.3"

scalaVersion := "2.13.16"

// javacOptions ++= Seq("-source", "11", "-target", "11")
// scalacOptions ++= Seq("-release:11")

import scala.sys.process.*

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    // buildInfo
    buildInfoOptions += BuildInfoOption.BuildTime,
    buildInfoKeys := Seq[BuildInfoKey](
      name,
      version,
      scalaVersion,
      sbtVersion,
      BuildInfoKey.action("gitBranch") {
        "git rev-parse --abbrev-ref HEAD".!!.trim
      },
      BuildInfoKey.action("gitCommit") {
        "git rev-parse HEAD".!!.trim
      }
    ),
    buildInfoPackage := "dev.pompilius",
    buildInfoUsePackageAsPath := true,
    ScapegoatCommon.scapegoatSettings
  )

play.sbt.routes.RoutesKeys.routesImport ++= Seq(
  "dev.tnr.elback.shared.domain.Pagination",
  "dev.tnr.elback.shared.infrastructure.binders.PaginationBinder.paginationBinder"
)

libraryDependencies ++= Seq(
  guice,
  jdbc,
  ehcache,
  ws,
  evolutions,
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,
  "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
  "org.mockito" % "mockito-core" % "5.18.0" % Test
)

lazy val scalikeJdbcVersion = "4.3.4"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "8.0.33",
  "org.scalikejdbc" %% "scalikejdbc" % scalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-config" % scalikeJdbcVersion,
  "org.scalikejdbc" %% "scalikejdbc-test" % scalikeJdbcVersion % Test,
  "org.scalikejdbc" %% "scalikejdbc-joda-time" % scalikeJdbcVersion
)

// https://mvnrepository.com/artifact/commons-codec/commons-codec
libraryDependencies += "commons-codec" % "commons-codec" % "1.19.0"

// https://github.com/lloydmeta/enumeratum
libraryDependencies ++= Seq(
  "com.beachape" %% "enumeratum" % "1.9.0",
  "com.beachape" %% "enumeratum-play" % "1.9.0",
  "com.beachape" %% "enumeratum-play-json" % "1.9.0"
)

// Jsoup for web scraping
libraryDependencies += "org.jsoup" % "jsoup" % "1.21.1"

// https://mvnrepository.com/artifact/org.apache.commons/commons-text
libraryDependencies += "org.apache.commons" % "commons-text" % "1.14.0"

// https://poi.apache.org/
libraryDependencies += "org.apache.poi" % "poi" % "5.4.1"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "5.4.1"

// https://mvnrepository.com/artifact/biz.paluch.logging/logstash-gelf
libraryDependencies += "biz.paluch.logging" % "logstash-gelf" % "1.15.1"

// https://mvnrepository.com/artifact/io.github.play-swagger/play-swagger
libraryDependencies += "io.github.play-swagger" %% "play-swagger" % "2.0.4"

libraryDependencies += "org.webjars" % "swagger-ui" % "5.26.2"

// Para solucionar problemas de dependencias con scala-parser-combinators entre Play y ScalikeJDBC
dependencyOverrides += "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0"

Compile / doc / sources := Seq.empty
Compile / packageDoc / publishArtifact := false
Test / run / javaOptions += "-Duser.timezone=GMT"
Test / javaOptions += "-Dlogger.resource=logback-test.xml"

// Allow to override the test configuration from the command line
Option(System.getProperty("test.config.resource")) match {
  case Some(resource) => Test / javaOptions += s"-Dconfig.resource=$resource"
  case _ =>
    Option(System.getProperty("test.config.file")) match {
      case Some(file) =>
        Test / javaOptions += s"-Dconfig.file=$file"
      case _ =>
        Test / javaOptions += "-Dconfig.resource=application-test.conf"
    }
}

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-deprecation",
  "-explaintypes",
  "-feature",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-unused:implicits"
  //  "-Ywarn-unused:imports",
  //  "-Ywarn-unused:privates"
)
