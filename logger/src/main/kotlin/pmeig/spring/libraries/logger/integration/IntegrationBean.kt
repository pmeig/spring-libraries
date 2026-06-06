package pmeig.spring.libraries.logger.integration

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.dsl.IntegrationFlow
import pmeig.spring.libraries.logger.integration.configurer.LoggerFlowConfigurer
import pmeig.spring.libraries.logger.logChannel
import java.util.concurrent.Executors

@Configuration
class IntegrationBean {
  @Bean("logChannel")
  fun logChannel() = logChannel

  @Bean
  fun logFlow(
    configurers: List<LoggerFlowConfigurer>,
    @Qualifier("logChannel") logChannel: DirectChannel
  ) = IntegrationFlow.from(logChannel).publishSubscribeChannel(Executors.newVirtualThreadPerTaskExecutor()) {
    configurers.forEach { configurer ->
      it.subscribe { flow -> configurer.configure(flow) }
    }
  }.get()
}