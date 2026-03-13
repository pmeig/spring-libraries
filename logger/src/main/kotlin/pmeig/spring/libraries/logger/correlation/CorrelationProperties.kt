package pmeig.spring.libraries.logger.correlation

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "spring.plugins.pmeig.logger.correlation")
@Configuration
class CorrelationProperties {
  var header: String = "X-Correlation-ID"
  var request: String = "correlationId"
}