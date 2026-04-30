package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.CacheManager

interface DataCacheNames {
  companion object {
    const val MAPPER_FIELDS = "data-mapper-fields"
    const val METADATA = "data-metadata"
    const val COLUMNS = "data-columns"

    fun <T : Any> useCache(
      cacheManager: CacheManager, name: String,
      key: String, target: Class<T>, compute: () -> T
    ): T {
      var cached = cacheManager.getCache(name)?.get(key, target)
      if (cached == null) {
        cached = compute()
        cacheManager.getCache(name)?.put(key, cached)
      }
      return cached
    }
  }
}