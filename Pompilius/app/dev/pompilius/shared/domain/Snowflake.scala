package dev.pompilius.shared.domain

import java.util.concurrent.atomic.AtomicLong

trait SnowflakeId {
  def id: Long

  //Para convertir el long en base 36, que contiene números y letras, y es más compacto que el decimal.
  override def toString: String = java.lang.Long.toString(id, 36)
}

trait Snowflake {

  // 2020-01-01T00:00:00+00:00
  private val epoch: Long = 0x16f5e66e800L
  private val snowflakeSeq = new AtomicLong(0)

  //  Preguntar aquí, porque al no tener nodo se le puede colocar un número aleatorio, o un número fijo, o un número basado en la hora, etc. Lo importante es que sea único para cada instancia de la aplicación.
  def genId(node: Int): Long = {
    if (node < 0 || node > 63)
      throw new IllegalArgumentException("Node ID must be between 0 and 63")

    val timestamp = System.currentTimeMillis()
    val seq = snowflakeSeq.incrementAndGet

    buildId(timestamp, node, seq)
  }

  def buildId(timestamp: Long, node: Int, seq: Long): Long = {
    // |  1 bit   |  41 bits  | 6 bits |  16 bits |
    // | Fixed 0  | Timestamp |  Node  | Sequence |
    (((timestamp - epoch) & 0x1ffffffffffL) << 22) | ((node & 0x3f) << 16) | (seq & 0xffff)
  }

  def parseId(s: String): Long = java.lang.Long.parseLong(s, 36)
}
