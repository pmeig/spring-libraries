package pmeig.spring.libraries.logger.correlation.http

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.webclient.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.correlationId

@Configuration
@ConditionalOnClass(WebClient::class)
class WebClientConfiguration(private val properties: CorrelationProperties) {

  @Bean
  fun correlationInterceptor(): WebClientCustomizer = WebClientCustomizer {
    it.filter { request, next ->
      next.exchange(
        ClientRequest.from(request)
          .header(properties.header, correlationId.toString()).build()
      )
    }
  }
}