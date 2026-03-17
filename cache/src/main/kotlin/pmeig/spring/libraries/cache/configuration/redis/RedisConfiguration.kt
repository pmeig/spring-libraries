package pmeig.spring.libraries.cache.configuration.redis

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import pmeig.spring.libraries.cache.CacheConfigUpdater
import pmeig.spring.libraries.cache.applyConfig
import pmeig.spring.libraries.cache.core.CacheConfigsProvider
import pmeig.spring.libraries.cache.core.model.CacheConfig
import java.time.Duration

@Configuration
@ConditionalOnClass(RedisCacheConfiguration::class)
class RedisConfiguration(private val redisHandler: RedisCacheHandler) {

  @Bean
  fun redisCacheConfigurationBuilder(
    cacheConfigsProvider: CacheConfigsProvider,
  ): RedisCacheManagerBuilderCustomizer = {
    val defaultConfiguration = redisHandler.defaultConfig(it.cacheDefaults())
    it.cacheDefaults(defaultConfiguration)
    cacheConfigsProvider.configs
      .forEach { cacheConfig ->
      it.withCacheConfiguration(
        cacheConfig.name, redisHandler.config(
          cacheConfig.name, createConfiguration(cacheConfig, defaultConfiguration)
        )
      )
    }

  }

  private fun createConfiguration(
    cacheConfig: CacheConfig,
    defaultConfiguration: RedisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
  ) = applyConfig(cacheConfig, RedisUpdater(defaultConfiguration)).config
}

private class RedisUpdater(var config: RedisCacheConfiguration): CacheConfigUpdater {
  override fun prefix(prefix: String) {
    config = config.prefixCacheNameWith(prefix)
  }
  override fun ttl(ttl: Duration) {
    config = config.entryTtl(ttl)
  }
  override fun notNullValue() {
    config = config.disableCachingNullValues()
  }
  override fun notUseKeyPrefix() {
    config = config.disableKeyPrefix()
  }
  override fun enableTimeToIdle() {
    config = config.enableTimeToIdle()
  }
}