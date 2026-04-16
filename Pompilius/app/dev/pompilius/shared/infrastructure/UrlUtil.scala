package dev.pompilius.shared.infrastructure

import org.apache.pekko.http.scaladsl.model.Uri
import org.apache.pekko.http.scaladsl.model.Uri.Query

object UrlUtil {

  def addQueryParameters(
      url: String,
      parameters: Map[String, String]
  ): String = {
    val uri = Uri(url)
    uri.withQuery(Query(uri.query().toMap ++ parameters)).toString
  }
  // Para evitar problema con *.toMap cuando hay parámetros repetidos
  def addQueryParameters(
      url: String,
      parameters: List[(String, String)]
  ): String = {
    val uri = Uri(url)
    uri.withQuery(Query(uri.query().toList ++ parameters: _*)).toString
  }

  def removeQueryParameters(url: String, parameters: List[String]): String = {
    val uri = Uri(url)
    uri.withQuery(Query(uri.query().toMap -- parameters)).toString
  }

  def redactUrl(url: String): String = {
    val uri = Uri(url)
    uri
      .withQuery(Query(uri.query().toMap.map {
        case (key, _) if key.toLowerCase.contains("password") => (key, "********")
        case (key, _) if key.toLowerCase.contains("token")    => (key, "********")
        case (key, _) if key.toLowerCase.contains("key")      => (key, "********")
        case (key, _) if key.toLowerCase.contains("apikey")   => (key, "********")
        case (key, _) if key.toLowerCase.contains("api-key")  => (key, "********")
        case (key, _) if key.toLowerCase.contains("secret")   => (key, "********")
        case (key, _) if key.toLowerCase.contains("code")     => (key, "********")
        case (key, _) if key.toLowerCase.contains("pin")      => (key, "********")
        case (key, value)                                     => (key, value)
      }))
      .toString
  }

  def interpolateVariables(
      url: String,
      parameters: Map[String, String]
  ): String = {
    parameters.foldLeft(url) {
      case (s, (key, value)) => s.replace(s"$${$key}", value)
    }
  }

  def isValidRelativeUrl(url: String): Boolean = {
    url.matches("""^[a-zA-Z0-9_\-\.\/]+$""")
  }

}
