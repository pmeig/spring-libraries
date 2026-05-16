package pmeig.spring.libraries.cache.configuration.hazelcast

import com.hazelcast.config.Config
import com.hazelcast.config.MapConfig
import com.hazelcast.config.NetworkConfig
import com.hazelcast.core.Hazelcast
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.spring.cache.HazelcastCacheManager
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.cache.CacheConfigUpdater
import pmeig.spring.libraries.cache.applyConfig
import pmeig.spring.libraries.cache.core.CacheConfigsProvider
import java.time.Duration

@Configuration
@ConditionalOnClass(HazelcastCacheManager::class)
class HazelcastConfiguration : BeanPostProcessor, ApplicationContextAware {

  private lateinit var applicationContext: ApplicationContext

  @Bean("defaultHazelcastConfig")
  @ConditionalOnMissingBean(Config::class)
  fun config(
    hazelcastProperties: HazelcastConfigProperties
  ): Config {
    val config = Config()
    hazelcastProperties.cluster?.let { config.clusterName = it }
    config.properties = hazelcastProperties.properties
    config.networkConfig = createNetworkConfig(hazelcastProperties)
    return config
  }

  @Bean
  @ConditionalOnMissingBean(HazelcastCacheManager::class)
  fun hazelcastCacheManager(hazelcastInstance: HazelcastInstance) = HazelcastCacheManager(hazelcastInstance)

  @Bean
  @ConditionalOnMissingBean(HazelcastInstance::class)
  fun hazelcastInstance(config: Config): HazelcastInstance? = Hazelcast.newHazelcastInstance(config)

  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.applicationContext = applicationContext
  }

  override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
    if (bean is HazelcastInstance) {
      val cacheHandler = applicationContext.getBean<HazelcastCacheHandler>()
      cacheHandler.defaultConfig(bean.config)
      val updater = HazelcastConfigUpdater(bean.config)
      applicationContext.getBean<CacheConfigsProvider>().configs.forEach { cacheConfig ->
        cacheHandler.config(cacheConfig.name, applyConfig(cacheConfig, updater).config)
      }
    }
    return bean
  }

  private fun createNetworkConfig(network: HazelcastConfigProperties): NetworkConfig {
    val networkConfig = NetworkConfig()
    networkConfig.port = network.port
    networkConfig.publicAddress = network.addresses.joinToString(",")
    return networkConfig
  }
}

private class HazelcastConfigUpdater(val config: Config) : CacheConfigUpdater {
  override fun prefix(prefix: String) {
  }

  override fun ttl(ttl: Duration) {
    config.addMapConfig(MapConfig().apply {
      timeToLiveSeconds = ttl.seconds.toInt()
    })
  }

  override fun notNullValue() {

  }

  override fun notUseKeyPrefix() {
  }

  override fun enableTimeToIdle() {
  }
}