package pmeig.spring.libraries.security.core.authorization

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.security.core.authorization.provider.SecurityAuthorizationProvider

@Suppress("unused")
@ConfigurationProperties("spring.security.pmeig.auth")
@Configuration
class SecurityAuthorizationProperties(
  var type: SecurityAuthenticateType = SecurityAuthenticateType.JWT,
  var login: Boolean = false,
  var configs: List<SecurityAuthorization> = listOf()
): SecurityAuthorizationProvider {
  override fun get(): Collection<SecurityAuthorization> = configs
}