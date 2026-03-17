package pmeig.spring.libraries.cache.core.model

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "spring.cache")
@Configuration
class CacheConfigsProperties(
  var configs: Map<String, CacheConfig> = emptyMap()
)