package pmeig.spring.libraries.security.core.models

import pmeig.spring.libraries.security.core.annotation.PmeigSecurity

data class SecurityFeatures(
  val features: List<String> = listOf(),
  val type: PmeigSecurity.Type = PmeigSecurity.Type.OR,
)
