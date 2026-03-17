package pmeig.spring.libraries.cache

import pmeig.spring.libraries.cache.core.model.CacheConfig
import java.time.Duration

internal fun <T: CacheConfigUpdater> applyConfig(cacheConfig: CacheConfig, updater: T): T {
  cacheConfig.keyPrefix?.let {
    updater.prefix(it)
  }
  cacheConfig.ttl?.let {
    updater.ttl(it)
  }
  if (!cacheConfig.nullable) {
    updater.notNullValue()
  }
  if (cacheConfig.idle) {
    updater.enableTimeToIdle()
  }
  if (!cacheConfig.useKeyPrefix) {
    updater.notUseKeyPrefix()
  }
  return updater
}

internal interface CacheConfigUpdater {
  fun prefix(prefix: String)
  fun ttl(ttl: Duration)
  fun notNullValue()
  fun notUseKeyPrefix()
  fun enableTimeToIdle()
}
