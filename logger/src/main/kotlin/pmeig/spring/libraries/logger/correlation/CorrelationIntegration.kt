package pmeig.spring.libraries.logger.correlation

import org.springframework.stereotype.Component
import pmeig.spring.libraries.logger.integration.configurer.log.argument.LoggerArgumentProvider
import pmeig.spring.libraries.logger.integration.configurer.log.mdc.MDCConfigurer

@Component
class CorrelationIntegration: LoggerArgumentProvider, MDCConfigurer {
  override fun provide(argument: String) = if ("correlation" == argument) correlationId.toString() else null

  override fun configure(): Map<String, String> {
    return mapOf("correlation" to correlationId.toString())
  }
}