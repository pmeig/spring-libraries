package pmeig.spring.libraries.jpa.core.cache

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass
import org.springframework.cache.CacheManager
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnMissingBean(CacheManager::class)
class DataCacheConfiguration {

  @ConditionalOnClass(CaffeineCacheManager::class)
  @Bean
  fun caffeineCacheManager(): CacheManager {
    return CaffeineCacheManager()
  }

  @ConditionalOnMissingClass("org.springframework.cache.caffeine.CaffeineCacheManager")
  @Bean
  fun defaultCacheManager(): CacheManager {
    return LocalCacheManager()
  }
}