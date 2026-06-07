package pmeig.spring.libraries.security.authentication.jwt.handler

import io.jsonwebtoken.JwtBuilder
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.JwtParserBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.authentication.jwt.JwtProperties
import java.security.PublicKey
import java.util.Date
import javax.crypto.SecretKey
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@Component
@ConditionalOnMissingBean(JwtConfigurer::class)
class JwtPropertiesConfigurer(private val properties: JwtProperties): JwtConfigurer {
  override fun prepareCreateToken(builder: JwtBuilder): JwtBuilder = builder.issuer(properties.issuer)
    .expiration(dateExpiration())
    .subject(properties.subject).signWith(properties.keyPair().private)

  override fun prepareParseToken(
    token: String,
    parser: JwtParserBuilder
  ): JwtParserBuilder {
    val key = properties.keyPair().public
    try {
      var builder = parser.requireIssuer(properties.issuer).requireSubject(properties.subject).requireExpiration(dateExpiration())
      builder = if (key is PublicKey) {
        builder.verifyWith(key)
      } else {
        builder.verifyWith(key as SecretKey)
      }
      return builder
    } catch (_: JwtException) {
      return parser
    }
  }

  private fun dateExpiration() = Date.from(Instant.fromEpochMilliseconds(properties.exp.toMillis()).toJavaInstant())
}