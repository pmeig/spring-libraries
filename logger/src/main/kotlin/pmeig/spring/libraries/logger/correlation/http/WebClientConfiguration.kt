package pmeig.spring.libraries.logger.correlation.http

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.WebClient
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.correlationId

@Configuration
@ConditionalOnClass(WebClient::class)
class WebClientConfiguration(private val properties: CorrelationProperties) : BeanPostProcessor {

  override fun postProcessAfterInitialization(bean: Any, beanName: String) = if (bean is WebClient)
    bean.mutate().filter { request, next ->
     next.exchange(ClientRequest.from(request)
       .header(properties.header, correlationId.toString()).build())
    }
  else super.postProcessAfterInitialization(bean, beanName)
}