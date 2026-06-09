package pmeig.spring.libraries.logger.integration.configurer

import org.springframework.integration.dsl.IntegrationFlowDefinition

fun interface LoggerFlowConfigurer {
  fun configure(flow: IntegrationFlowDefinition<*>)
}