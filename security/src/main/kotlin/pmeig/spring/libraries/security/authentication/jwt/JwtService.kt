package pmeig.spring.libraries.security.authentication.jwt

import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service
import pmeig.spring.libraries.security.authentication.jwt.handler.JwtClaimHandler
import pmeig.spring.libraries.security.authentication.jwt.handler.JwtConfigurer
import pmeig.spring.libraries.security.authentication.jwt.model.RequestUser

@Service
class JwtService(
  private val configurer: JwtConfigurer,
  private val userHandler: JwtClaimHandler<UserDetails>
) {
  fun createToken(user: RequestUser): String? {
    val builder = configurer.prepareCreateToken(Jwts.builder())
    return userHandler.parse(
      builder.claims(), user
    ).and().compact()
  }

  fun unparse(token: String): UserDetails? {
    val parserBuilder = configurer.prepareParseToken(token, Jwts.parser())
    return try {
      userHandler.unparse(parserBuilder.build().parseSignedClaims(token).payload)
    } catch (_: JwtException) {
      null
    }

  }
}