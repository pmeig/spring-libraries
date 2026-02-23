package pmeig.spring.libraries.security.authentication.jwt.handler

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.core.crypto.CryptoService

@Component
@ConditionalOnMissingBean(JwtClaimHandler::class)
class DefaultJwtClaimHandler(private val cryptoService: CryptoService): JwtClaimHandler<UserDetails> {

  override fun parse(
    builder: JwtBuilder.BuilderClaims,
    user: UserDetails
  ): JwtBuilder.BuilderClaims {
    return builder.add("username", user.username)
      .add("roles", user.authorities.map { it.authority })
      .add("credential", cryptoService.encrypt(user.password))
  }

  override fun unparse(claims: Claims): UserDetails {
    val username = claims["username"] as String
    @Suppress("UNCHECKED_CAST") val roles = claims["roles"] as List<String>
    val credential = claims["credential"] as String
    val password = cryptoService.decrypt(credential)
    return User(username, password.ifEmpty { null }, roles.map { SimpleGrantedAuthority(it) })
  }
}