package pmeig.spring.libraries.cache.core.model

import java.time.Duration

data class CacheConfig(
  var ttl: Duration? = null,
  var name: String = "",
  var keyPrefix: String? = null,
  var nullable: Boolean = false,
  var useKeyPrefix: Boolean = true,
  var idle: Boolean = false

)