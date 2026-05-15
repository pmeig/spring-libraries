package pmeig.spring.libraries.jpa.core.cache

import org.springframework.cache.CacheManager
import pmeig.spring.libraries.jpa.core.entity.model.DataMetadata
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryFieldMapper

interface DataCacheNames {
  companion object {
    const val MAPPER_FIELDS = "data-mapper-fields"
    const val METADATA = "data-metadata"
    const val COLUMNS = "data-columns"

    @JvmStatic
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

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun mappers(cacheManager: CacheManager, metadata: DataMetadata, compute: () -> Map<String, BigQueryFieldMapper<*>>): Map<String, BigQueryFieldMapper<*>> = useCache(cacheManager, MAPPER_FIELDS,
      metadata.reference.typeName + "_" + metadata.columns.keys.joinToString("-"), Map::class.java, compute) as Map<String, BigQueryFieldMapper<*>>
  }
}