package pmeig.spring.libraries.jpa.core.converter.configuration.zoneId

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.jpa.core.zoneIdProviderInstance
import java.time.ZoneOffset


@Configuration
class DataZoneIdConfiguration: BeanPostProcessor {
  private var saveZoneIdProvider: (bean: Any) -> Any = { bean ->
    if (bean is DataZoneIdProvider) {
      zoneIdProviderInstance = bean
      saveZoneIdProvider = { it }
    }
    bean
  }

  @Bean
  @ConditionalOnMissingBean(DataZoneIdProvider::class)
  fun defaultDataZoneIdProvider(): DataZoneIdProvider = DataZoneIdProvider { ZoneOffset.UTC }

  override fun postProcessAfterInitialization(bean: Any, beanName: String) = saveZoneIdProvider(bean)
}