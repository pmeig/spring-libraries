package pmeig.spring.libraries.security.core.authorization

@FunctionalInterface
interface SecurityAuthorizationProvider {
  fun get(): Collection<SecurityAuthorization>
}
