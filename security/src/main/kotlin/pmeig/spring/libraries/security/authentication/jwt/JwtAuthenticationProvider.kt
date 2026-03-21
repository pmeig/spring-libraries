package pmeig.spring.libraries.security.authentication.jwt

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import pmeig.spring.libraries.security.core.AuthenticationFactory
import pmeig.spring.libraries.security.core.filter.SecurityAuthenticationProvider
import pmeig.spring.libraries.security.core.manager.TokenManager
import pmeig.spring.libraries.security.core.models.api.ApiRequest

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class JwtAuthenticationProvider(private val tokenManager: TokenManager,
  private val jwtService: JwtService,
  private val authenticationFactory: AuthenticationFactory<UserDetails>): SecurityAuthenticationProvider {

  override fun from(
    request: ApiRequest,
    previous: Authentication?
  ): Authentication? {
    if (null != previous) return previous
    val token = tokenManager.getToken(request) ?: return null
    val user = jwtService.unparse(token) ?: return null
    val authentication = authenticationFactory.from(user)
    return authentication
  }
}