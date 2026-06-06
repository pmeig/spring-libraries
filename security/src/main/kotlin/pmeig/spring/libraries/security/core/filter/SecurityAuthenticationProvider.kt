package pmeig.spring.libraries.security.core.filter

import org.springframework.security.core.Authentication
import pmeig.spring.libraries.security.core.model.api.ApiRequest


fun interface SecurityAuthenticationProvider {
  fun from(request: ApiRequest, previous: Authentication?): Authentication?
}