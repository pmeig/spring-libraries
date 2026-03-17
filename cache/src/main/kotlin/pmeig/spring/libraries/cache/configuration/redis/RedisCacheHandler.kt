package pmeig.spring.libraries.cache.configuration.redis

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.stereotype.Component
import pmeig.spring.libraries.cache.core.CacheHandler
import tools.jackson.databind.ObjectMapper

@Component
@ConditionalOnMissingBean(CacheHandler::class)
@ConditionalOnClass(RedisCacheConfiguration::class)
@ConditionalOnProperty(prefix = "spring.cache", name = ["type"], havingValue = "redis", matchIfMissing = true)
class RedisCacheHandler(
  private val mapper: ObjectMapper
): CacheHandler<RedisCacheConfiguration> {
  override fun defaultConfig(config: RedisCacheConfiguration): RedisCacheConfiguration {
    return super.defaultConfig(config.serializeValuesWith(
      RedisSerializationContext.SerializationPair.fromSerializer(
        GenericJacksonJsonRedisSerializer(mapper)
      )).disableCachingNullValues())
  }
}