package pmeig.spring.libraries.security.authentication.jwt.handler

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.authentication.jwt.model.RequestUser
import pmeig.spring.libraries.security.core.crypto.CryptoService

@Suppress("RedundantModalityModifier")
@Component
@ConditionalOnMissingBean(JwtClaimHandler::class)
open class JwtClaimHandler<T: UserDetails>(protected val cryptoService: CryptoService) {

  open fun parse(
    builder: JwtBuilder.BuilderClaims,
    user: T
  ): JwtBuilder.BuilderClaims {
    return builder.add("username", user.username)
      .add("roles", user.authorities.map { it.authority })
      .add("credential", cryptoService.encrypt(user.password))
  }

  @Suppress("UNCHECKED_CAST")
  open fun parse(builder: JwtBuilder.BuilderClaims, user: RequestUser)
          = parse(builder, User(user.pseudo, user.credential, listOf()) as T)

  @Suppress("UNCHECKED_CAST")
  open fun unparse(claims: Claims): T {
    val username = claims["username"] as String
    @Suppress("UNCHECKED_CAST") val roles = claims["roles"] as List<String>
    val credential = claims["credential"] as String
    val password = cryptoService.decrypt(credential)
    return User(username, password.ifEmpty { null }, roles.map { SimpleGrantedAuthority(it) }) as T
  }
}