package dev.pompilius.shared.infrastructure

import org.apache.commons.codec.binary.Base64

import java.io.{File, FileInputStream, FileOutputStream, InputStream}
import java.nio.charset.StandardCharsets
import java.security._
import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import javax.crypto.spec.{GCMParameterSpec, IvParameterSpec, PBEKeySpec, SecretKeySpec}
import javax.crypto.{Cipher, CipherInputStream, CipherOutputStream, SecretKeyFactory}
import scala.util.Using

object CipherUtil {

  private val secureRandom = new SecureRandom()

  private val KeyGenerationAlgorithm = "PBKDF2WithHmacSHA256"
  private val KeyLength = 256
  private val Iterations = 65536

  private val RSAKeySize = 2048
  private val RSAKeyFactoryAlgorithm = "RSA"
  private val RSACipherAlgorithm = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"

  private val TextCipherAlgorithm = "AES/GCM/NoPadding"
  private val TextSaltLength = 16
  private val TextIvLength = 12
  private val StreamCipherAlgorithm = "AES/CBC/PKCS5Padding"
  private val StreamSaltLength = 16
  private val StreamIvLength = 16

  private val BufferLength = 4096

  private def encode(bytes: Array[Byte]): String = {
    new Base64(0, Array.empty[Byte], true).encodeToString(bytes)
  }

  private def decode(text: String): Array[Byte] = {
    Base64.decodeBase64(text)
  }

  private def deriveKey(password: String, salt: Array[Byte]): Key = {
    new SecretKeySpec(
      SecretKeyFactory
        .getInstance(KeyGenerationAlgorithm)
        .generateSecret(new PBEKeySpec(password.toCharArray, salt, Iterations, KeyLength))
        .getEncoded,
      "AES"
    )
  }

  def generateRSAKeyPair: KeyPair = {
    val keyGen = KeyPairGenerator.getInstance(RSAKeyFactoryAlgorithm)
    keyGen.initialize(RSAKeySize)
    keyGen.generateKeyPair()
  }

  def keyToString(key: Key): String = {
    encode(key.getEncoded)
  }

  def publicKeyFromString(key: String): PublicKey = {
    val bytes = Base64.decodeBase64(key)
    val spec = new X509EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance(RSAKeyFactoryAlgorithm)
    keyFactory.generatePublic(spec)
  }

  def privateKeyFromString(key: String): PrivateKey = {
    val bytes = Base64.decodeBase64(key)
    val spec = new PKCS8EncodedKeySpec(bytes)
    val keyFactory = KeyFactory.getInstance(RSAKeyFactoryAlgorithm)
    keyFactory.generatePrivate(spec)
  }

  def randomPassword(length: Int = 16): String = {
    val password = new Array[Byte](length)
    secureRandom.nextBytes(password)
    encode(password)
  }

  def randomToken(length: Int = 32): String = {
    val token = new Array[Byte](length)
    secureRandom.nextBytes(token)
    encode(token)
  }

  private def alphanumeric: LazyList[Char] = {
    def nextAlphaNum: Char = {
      val chars = "BCDFGHJKLMNPQRSTVWXYZ23456789"
      chars charAt (secureRandom nextInt chars.length)
    }

    LazyList continually nextAlphaNum
  }

  private def numeric: LazyList[Char] = {
    def nextAlphaNum: Char = {
      val chars = "23456789"
      chars charAt (secureRandom nextInt chars.length)
    }

    LazyList continually nextAlphaNum
  }

  def randomCode(length: Int = 6): String = {
    alphanumeric.take(length).mkString
  }

  // Low security
  def randomPin(length: Int = 6): String = {
    numeric.take(length).mkString
  }

  def encrypt(text: String, password: String): String = {

    val salt = new Array[Byte](TextSaltLength)
    secureRandom.nextBytes(salt)

    val iv = new Array[Byte](TextIvLength)
    secureRandom.nextBytes(iv)

    val key = deriveKey(password, salt)

    val cipher = javax.crypto.Cipher.getInstance(TextCipherAlgorithm)
    cipher.init(
      Cipher.ENCRYPT_MODE,
      key,
      // Tag length is 128, IV length is 96
      new GCMParameterSpec(128, iv)
    )

    val encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8))

    s"${encode(salt)}.${encode(iv)}.${encode(encrypted)}"
  }

  def decrypt(secret: String, password: String): String = {
    secret.split('.') match {
      case Array(salt, iv, encrypted) =>
        val key = deriveKey(password, decode(salt))
        val cipher = Cipher.getInstance(TextCipherAlgorithm)
        cipher.init(
          Cipher.DECRYPT_MODE,
          key,
          // Tag length is 128, IV length is 96
          new GCMParameterSpec(128, decode(iv))
        )
        new String(cipher.doFinal(decode(encrypted)), StandardCharsets.UTF_8)

      case _ =>
        throw new IllegalArgumentException("Invalid secret format")
    }
  }

  private def encryptStream(inputStream: InputStream, outputFile: File, password: String): Unit = {
    val salt = new Array[Byte](StreamSaltLength)
    secureRandom.nextBytes(salt)

    val iv = new Array[Byte](StreamIvLength)
    secureRandom.nextBytes(iv)

    val key = deriveKey(password, salt)
    val cipher = Cipher.getInstance(StreamCipherAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv))

    Using.resource(new FileOutputStream(outputFile)) { fileOutputStream =>
      // Escribir salt y IV al archivo
      fileOutputStream.write(salt)
      fileOutputStream.write(iv)

      Using.resource(new CipherOutputStream(fileOutputStream, cipher)) { cipherOutputStream =>
        val buffer = new Array[Byte](BufferLength)
        Iterator
          .continually(inputStream.read(buffer))
          .takeWhile(_ != -1)
          .foreach(cipherOutputStream.write(buffer, 0, _))
      }
    }
  }

  def encryptFile(inputFile: File, outputFile: File, password: String): Unit = {
    Using.resource(new FileInputStream(inputFile)) { inputStream =>
      encryptStream(inputStream, outputFile, password)
    }
  }

  def encryptBase64EncodedFile(inputFile: File, outputFile: File, password: String): Unit = {
    Using.resource(new FileInputStream(inputFile)) { inputStream =>
      Using.resource(java.util.Base64.getMimeDecoder.wrap(inputStream)) { base64InputStream =>
        encryptStream(base64InputStream, outputFile, password)
      }
    }
  }

  def decryptFile(inputFile: File, outputFile: File, password: String): Unit = {
    Using.resources(
      new FileInputStream(inputFile),
      new FileOutputStream(outputFile)
    ) { (fileInputStream, fileOutputStream) =>
      val salt = new Array[Byte](StreamSaltLength)
      val iv = new Array[Byte](StreamIvLength)

      fileInputStream.read(salt)
      fileInputStream.read(iv)

      val key = deriveKey(password, salt)
      val cipher = Cipher.getInstance(StreamCipherAlgorithm)
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv))

      Using.resource(new CipherInputStream(fileInputStream, cipher)) { cipherInputStream =>
        val buffer = new Array[Byte](BufferLength)
        Iterator
          .continually(cipherInputStream.read(buffer))
          .takeWhile(_ != -1)
          .foreach(fileOutputStream.write(buffer, 0, _))
      }
    }
  }

  def encryptWithPublicKey(text: String, key: PublicKey): String = {
    val cipher = Cipher.getInstance(RSACipherAlgorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    encode(cipher.doFinal(text.getBytes(StandardCharsets.UTF_8)))
  }

  def decryptWithPrivateKey(text: String, key: PrivateKey): String = {
    val cipher = Cipher.getInstance(RSACipherAlgorithm)
    cipher.init(Cipher.DECRYPT_MODE, key)
    new String(cipher.doFinal(Base64.decodeBase64(text)), StandardCharsets.UTF_8)
  }

}
