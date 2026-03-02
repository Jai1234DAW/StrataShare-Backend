package dev.pompilius.shared.infrastructure.contexts

import org.apache.pekko.actor.ActorSystem
import javax.inject.{Inject, Singleton}

@Singleton
class DbExecutionContext @Inject() (actorSystem: ActorSystem)
    extends CustomExecutionContext(actorSystem, "custom-execution-contexts.database")
