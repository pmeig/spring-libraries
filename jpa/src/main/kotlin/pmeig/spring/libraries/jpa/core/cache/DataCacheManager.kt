package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.stereotype.Component

@Component
class DataCacheManager(configurer: (CaffeineCacheManager) -> Unit = {}) {

  private val cacheManager: CacheManager = CaffeineCacheManager().apply {
    setCacheSpecification("expireAfterAccess=4h")
    isAllowNullValues = true
    configurer(this)
  }

  fun getCache(name: String): Cache? = cacheManager.getCache(name)

  @Suppress("unused")
  fun getCacheNames(): Collection<String> = cacheManager.cacheNames
}