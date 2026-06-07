package pmeig.spring.libraries.logger.correlation.http

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.restclient.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.correlationId

@Configuration
@ConditionalOnClass(RestClient::class)
class RestClientConfiguration(private val properties: CorrelationProperties) {

  @Bean
  fun restClientCorrelationInterceptor(): RestClientCustomizer = RestClientCustomizer {
    it.requestInterceptor { request, body, execution ->
      request.headers.add(properties.header,  correlationId.toString())
      execution.execute(request, body)
    }
  }
}