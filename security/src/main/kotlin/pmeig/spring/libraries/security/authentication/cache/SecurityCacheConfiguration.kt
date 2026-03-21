package pmeig.spring.libraries.security.authentication.cache

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.reactive.function.client.WebClient
import pmeig.spring.libraries.cache.core.model.CacheConfig
import pmeig.spring.libraries.logger.correlation.correlationId

@Configuration
class SecurityCacheConfiguration(
  private val cacheManager: CacheManager,
  private val userCacheProperties: UserCacheProperties
) {

  @Bean
  @ConditionalOnClass(CacheConfig::class)
  fun userCacheConfig(userCacheProperties: UserCacheProperties) =
    CacheConfig(
      userCacheProperties.ttl, userCacheProperties.name,
      userCacheProperties.keyPrefix, userCacheProperties.nullable, userCacheProperties.useKeyPrefix,
      userCacheProperties.idle
    )

  @Bean
  @ConditionalOnClass(WebClient::class)
  fun webClientInsertUser(): BeanPostProcessor = object : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
      if (bean is WebClient) {
        return bean.mutate().filter { request, next ->
          ReactiveSecurityContextHolder.getContext().flatMap {
            cacheManager.getCache(userCacheProperties.name)?.put(correlationId, it.authentication)
            next.exchange(request)
          }
        }.build()
      }
      return super.postProcessAfterInitialization(bean, beanName)
    }
  }
}