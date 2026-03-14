package pmeig.spring.libraries.security.core.authorization

import org.springframework.web.bind.annotation.RequestMethod
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity

data class SecurityAuthorization(
  var path: List<String> = listOf(),
  var methods: List<RequestMethod> = listOf(),
  var features: Set<String> = setOf(),
  var public: Boolean = false,
  var denied: Boolean = false,
  var contain: Boolean = true,
  var type: PmeigSecurity.Type = PmeigSecurity.Type.OR
)