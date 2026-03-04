package dev.pompilius.auth.domain

import dev.pompilius.shared.infrastructure.CipherUtil

case class SessionToken (token:String) {

  override def toString: String = token
}

object SessionToken {
  val length: Int=32
  def random: SessionToken=SessionToken(CipherUtil.randomToken(length).take(length))
}