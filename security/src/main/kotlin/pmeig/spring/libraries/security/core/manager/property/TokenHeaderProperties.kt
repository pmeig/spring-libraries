package pmeig.spring.libraries.security.core.manager.property

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("spring.security.pmeig.auth.token.header")
class TokenHeaderProperties(
  var prefix: String = "Bearer ",
  var name: String = "Authorization"
)
