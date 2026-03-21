package pmeig.spring.libraries.security.authentication.cache

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "spring.security.pmeig.auth.cache.config")
class UserCacheProperties(
  var ttl: Duration? = null,
  var name: String = "",
  var keyPrefix: String? = null,
  var nullable: Boolean = false,
  var useKeyPrefix: Boolean = true,
  var idle: Boolean = false
) {
}