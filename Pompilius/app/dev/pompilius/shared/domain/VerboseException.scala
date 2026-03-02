package dev.pompilius.shared.domain

import java.util.concurrent.atomic.AtomicLong

@SuppressWarnings(Array("NullAssignment"))
class VerboseException(val message: String, val logAsError: Boolean = false) extends Exception(message) {

  var id: String = VerboseException.nextId
}

object VerboseException {
  private val generator = new AtomicLong(System.currentTimeMillis())

  private def nextId: String = {
    java.lang.Long.toString(generator.incrementAndGet(), 36)
  }
}
