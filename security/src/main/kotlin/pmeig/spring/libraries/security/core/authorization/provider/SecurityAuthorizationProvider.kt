package pmeig.spring.libraries.security.core.authorization.provider

import pmeig.spring.libraries.security.core.authorization.SecurityAuthorization

@FunctionalInterface
interface SecurityAuthorizationProvider {
  fun get(): Collection<SecurityAuthorization>
}