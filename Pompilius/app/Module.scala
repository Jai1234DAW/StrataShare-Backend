import com.google.inject.AbstractModule
import com.google.inject.name.Names
import dev.pompilius.shared.infrastructure.ScalikejdbcAdapter
import play.api.{Configuration, Environment}

@SuppressWarnings(Array("UnusedMethodParameter"))
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {

    bind(classOf[ScalikejdbcAdapter]).asEagerSingleton()

//    bind(classOf[AccountRepository])
//      .annotatedWith(Names.named("No cached"))
//      .to(classOf[AccountMySqlRepository])
  }

}
