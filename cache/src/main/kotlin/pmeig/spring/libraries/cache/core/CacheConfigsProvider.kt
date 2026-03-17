package pmeig.spring.libraries.cache.core

import org.springframework.stereotype.Component
import pmeig.spring.libraries.cache.core.model.CacheConfig
import pmeig.spring.libraries.cache.core.model.CacheConfigsProperties

@Component
class CacheConfigsProvider(
  cacheConfigsProperties: CacheConfigsProperties,
  cacheConfigs: List<CacheConfig>) {

  val configs: List<CacheConfig> = cacheConfigs + cacheConfigsProperties.configs.map { (name, config) ->
    config.name = name
    config
  }
}