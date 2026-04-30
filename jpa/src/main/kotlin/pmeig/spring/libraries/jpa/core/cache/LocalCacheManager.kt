package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.Cache
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

class LocalCacheManager : ConcurrentMapCacheManager() {
  override fun createConcurrentMapCache(name: String): Cache {
    val cache = super.createConcurrentMapCache(name)
    return TTLCache(cache)
  }
}