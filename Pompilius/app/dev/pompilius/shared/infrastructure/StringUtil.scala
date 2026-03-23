package dev.pompilius.shared.infrastructure

import org.apache.commons.lang3.StringUtils.stripAccents
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist

object StringUtil {

  val MinSimilarity = 0.8

  // Longest common substring
  @SuppressWarnings(Array("UnsafeTraversableMethods"))
  def longestCommonSubstring(left: String, right: String): Int = {
    if (left.isEmpty || right.isEmpty) return 0
    val m = Array.ofDim[Int](left.length, right.length)
    for (i <- 0 until left.length) {
      for (j <- 0 until right.length) {
        if (left.charAt(i) == right.charAt(j)) {
          if (i == 0 || j == 0) m(i)(j) = 1 else m(i)(j) = m(i - 1)(j - 1) + 1
        } else {
          m(i)(j) = 0
        }
      }
    }
    m.map(_.max).max
  }

  def similarity(left: String, right: String): Double = {
    val normalizedLeft = normalize(left)
    val normalizedRight = normalize(right)
    val minLength = Math.min(normalizedLeft.length, normalizedRight.length)
    if (minLength > 0) {
      longestCommonSubstring(normalizedLeft, normalizedRight).toDouble / minLength.toDouble
    } else 0.0
  }

  def equal(left: String, right: String): Boolean = {
    val normalizedLeft = normalize(left)
    val normalizedRight = normalize(right)
    normalizedLeft == normalizedRight
  }

  def normalize(s: String): String = {
    stripAccents(s.toUpperCase).flatMap {
      case ' '  => None
      case '.'  => None
      case ','  => None
      case ':'  => None
      case ';'  => None
      case '-'  => None
      case '_'  => None
      case '\'' => None
      case '`'  => None
      case '´'  => None
      case '¨'  => None
      case '^'  => None
      case '+'  => None
      case '*'  => None
      case '<'  => None
      case '>'  => None
      case c    => Some(c)
    }.mkString
  }

  def findValue[T](s: String, minSimilarity: Option[Double], values: (String, T)*): Option[T] = {

    val fullMatch = values.find { value => equal(value._1, s) }.map(_._2)

    if (minSimilarity.exists(_ >= 1)) {
      fullMatch
    } else {
      fullMatch.orElse {
        values
          .map(value => (similarity(s, value._1), value._2))
          .filter(_._1 > minSimilarity.getOrElse(MinSimilarity))
          .sortBy(_._1)
          .headOption
          .map(_._2)
      }
    }
  }

  def toOption(s: String): Option[String] = {
    if (s.isEmpty) {
      None
    } else {
      Some(s)
    }
  }

  def maskDomain(domain: String): String = {
    domain.split("\\.") match {
      case Array(subdomain, tld) =>
        "*" * subdomain.length + "." + tld
      case _ =>
        "***.***"
    }
  }

  def maskEmail(email: String): String = {
    email.split("@") match {
      case Array(user, domain) if user.length > 6 =>
        user.take(2) + "*" * (user.length - 4) + user.takeRight(2) + "@" + maskDomain(domain)
      case Array(user, domain) if user.length > 3 =>
        user.take(1) + "*" * (user.length - 2)  + user.takeRight(1) + "@" + maskDomain(domain)
      case _ =>
        "***@***.***"
    }
  }

  def stripTags(s: String): String =
    Jsoup
      .clean(
        s,
        "",
        Safelist.none(),
        new Document.OutputSettings().prettyPrint(false)
      )
      .trim()

}
