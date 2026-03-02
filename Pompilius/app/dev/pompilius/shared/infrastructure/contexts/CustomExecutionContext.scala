package dev.pompilius.shared.infrastructure.contexts

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.dispatch.MessageDispatcher

import scala.concurrent.ExecutionContextExecutor

abstract class CustomExecutionContext(system: ActorSystem, name: String) extends ExecutionContextExecutor {
  private val dispatcher: MessageDispatcher = system.dispatchers.lookup(name)
  override def execute(command: Runnable): Unit = dispatcher.execute(command)
  override def reportFailure(cause: Throwable): Unit = dispatcher.reportFailure(cause)
}
