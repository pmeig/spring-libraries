package pmeig.spring.libraries.logger.integration.configurer.log.argument

import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment

@Configuration
@Order(Ordered.LOWEST_PRECEDENCE)
class LoggerPropertyProvider(
  private val environment: Environment
): LoggerArgumentProvider {

  override fun provide(argument: String): String? {
    return environment.getProperty(argument)
  }
}