package pmeig.spring.libraries.security.authentication.cache

import org.springframework.cache.CacheManager
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import pmeig.spring.libraries.logger.correlation.correlationId
import pmeig.spring.libraries.security.core.filter.SecurityAuthenticationProvider
import pmeig.spring.libraries.security.core.model.api.ApiRequest

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class SecurityCache(private val cacheManager: CacheManager,
  private val userCacheProperties: UserCacheProperties): SecurityAuthenticationProvider {
  override fun from(
    request: ApiRequest,
    previous: Authentication?
  ): Authentication? {
    return previous ?: cacheManager.getCache(userCacheProperties.name)?.get(correlationId) as Authentication?
  }
}