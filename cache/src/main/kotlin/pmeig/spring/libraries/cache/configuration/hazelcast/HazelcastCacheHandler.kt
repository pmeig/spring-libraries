package pmeig.spring.libraries.cache.configuration.hazelcast

import com.hazelcast.config.Config
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.cache.core.CacheHandler

@Configuration
@ConditionalOnMissingBean(HazelcastCacheHandler::class)
class HazelcastCacheHandler: CacheHandler<Config> {
  override fun defaultConfig(config: Config): Config {
    config.networkConfig.join.multicastConfig.isEnabled = true
    return super.defaultConfig(config)
  }
}