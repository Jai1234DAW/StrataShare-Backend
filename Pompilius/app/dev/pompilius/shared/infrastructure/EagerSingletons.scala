package dev.pompilius.shared.infrastructure

import com.google.inject.AbstractModule
import play.api.libs.concurrent.PekkoGuiceSupport
import play.api.{Configuration, Environment}

@SuppressWarnings(Array("UnusedMethodParameter"))
class EagerSingletons(environment: Environment, configuration: Configuration)
    extends AbstractModule
    with PekkoGuiceSupport {

  private val enabled =
    configuration
      .get[Seq[String]]("eagerSingletons.enabled")
      .distinct
      .diff(
        configuration.get[Seq[String]]("eagerSingletons.disabled").distinct
      )

  override def configure(): Unit = {

    enabled.foreach { className =>
      bind(Class.forName(className)).asEagerSingleton()
    }

  }
}
