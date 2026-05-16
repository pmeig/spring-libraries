package pmeig.spring.libraries.jpa.core.cache

interface DataCacheNames {
  companion object {
    const val METADATA = "data-metadata"
    const val COLUMNS = "data-columns"

    @JvmStatic
    fun <T : Any> useCache(
      cacheManager: DataCacheManager, name: String,
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