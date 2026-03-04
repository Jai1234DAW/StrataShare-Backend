package dev.pompilius.auth.domain

import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

case class MailToken(mail: String, expires: DateTime) {

  def signature(secretKey: SecretKeySpec): String = {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(secretKey)
    new Base64(0, Array.empty[Byte], true)
      .encodeAsString(
        mac.doFinal(s"$mail::${expires.getMillis}".getBytes("UTF-8"))
      )
  }
}