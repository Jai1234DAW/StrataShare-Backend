package dev.pompilius.auth.infrastructure.parsers

import dev.pompilius.auth.domain.MailToken
import dev.pompilius.shared.domain.exceptions.BadRequestException
import org.apache.commons.codec.binary.Base64
import org.joda.time.DateTime
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import javax.crypto.spec.SecretKeySpec

object MailTokenParser {

  def parse(s: String, secretKey: SecretKeySpec): MailToken = {

    s.split('.') match {
      case Array(payload, signature) =>
        val json = Json.parse(
          new String(
            new Base64(0, Array.empty[Byte], true)
              .decode(payload),
            StandardCharsets.UTF_8
          )
        )

        val token = MailToken(
          mail = (json \ "em").as[String],
          expires = new DateTime((json \ "ex").as[Long])
        )

        if (token.signature(secretKey) != signature) {
          throw new BadRequestException("Invalid signature")
        }

        token

      case _ =>
        throw new BadRequestException("Invalid token")
    }
  }

}
