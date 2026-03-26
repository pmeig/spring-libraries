package pmeig.spring.libraries.security.core.filter

import org.springframework.security.core.Authentication
import pmeig.spring.libraries.security.core.models.api.ApiRequest

@FunctionalInterface
interface SecurityAuthenticationProvider {
  fun from(request: ApiRequest, previous: Authentication? = null): Authentication?
}