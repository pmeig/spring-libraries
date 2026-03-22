package pmeig.spring.libraries.security.core.authorization.provider

import org.springframework.stereotype.Service
import pmeig.spring.libraries.security.core.authorization.SecurityAuthorization

@Service
class SecurityAuthorizationBeanProvider(private val securityAuthorizations: List<SecurityAuthorization>): SecurityAuthorizationProvider {
  override fun get() = securityAuthorizations
}