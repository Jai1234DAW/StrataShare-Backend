package dev.pompilius.users.domain

import org.apache.commons.codec.binary.Base64

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

case class UserPassword(password: String) {

  private val secureRandom = new SecureRandom()
  private val base64 = new Base64(0, Array.empty[Byte], true)

  override def toString: String = password

  def check(hash: String): Boolean = {
    hash.split(':') match {
      case Array(salt, iterationCount, derivedKey) =>
        val keyLength = 256

        base64.encodeAsString(
          PBKDF2WithHmacSHA256(
            password = password,
            salt = base64.decode(salt),
            iterationCount = java.lang.Integer.parseInt(iterationCount, 36),
            keyLength = keyLength
          )
        ) == derivedKey
      case _ =>
        throw new IllegalArgumentException(
          "Hash has an invalid format"
        )

    }
  }

  @SuppressWarnings(Array("MethodNames"))
  private def PBKDF2WithHmacSHA256(
      password: String,
      salt: Array[Byte],
      iterationCount: Int,
      keyLength: Int
  ): Array[Byte] = {
    SecretKeyFactory
      .getInstance("PBKDF2WithHmacSHA256")
      .generateSecret(
        new PBEKeySpec(password.toCharArray, salt, iterationCount, keyLength)
      )
      .getEncoded
  }

  def hash: String = {
    val salt = new Array[Byte](16)
    secureRandom.nextBytes(salt)

    val iterationCount = 200000
    val keyLength = 256
    val derivedKey =
      PBKDF2WithHmacSHA256(
        password = password,
        salt = salt,
        iterationCount = iterationCount,
        keyLength = keyLength
      )

    List(
      base64.encodeAsString(salt),
      java.lang.Integer.toString(iterationCount, 36),
      base64.encodeAsString(derivedKey)
    ).mkString(":")

  }
}

