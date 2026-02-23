package pmeig.spring.libraries.security.core.manager.property

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.plugins.pmeig.security.auth.token.header")
data class TokenHeaderProperties(
  var prefix: String = "Bearer ",
  var name: String = "Authorization"
)
