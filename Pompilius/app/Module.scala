import com.google.inject.AbstractModule
import dev.pompilius.badge.application.BadgeInitializer
import dev.pompilius.shared.infrastructure.ScalikejdbcAdapter
import play.api.{Configuration, Environment}

@SuppressWarnings(Array("UnusedMethodParameter"))
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[ScalikejdbcAdapter]).asEagerSingleton()

    // Inicializar sistema de badges al arranque
    bind(classOf[BadgeInitializer]).asEagerSingleton()

//    bind(classOf[AccountRepository])
//      .annotatedWith(Names.named("No cached"))
//      .to(classOf[AccountMySqlRepository])
  }

}
