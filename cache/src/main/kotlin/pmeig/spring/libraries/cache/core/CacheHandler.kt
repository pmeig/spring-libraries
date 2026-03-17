package pmeig.spring.libraries.cache.core

interface CacheHandler<T: Any> {
  fun defaultConfig(config: T): T = config
  fun config(name: String, config: T): T = config
}