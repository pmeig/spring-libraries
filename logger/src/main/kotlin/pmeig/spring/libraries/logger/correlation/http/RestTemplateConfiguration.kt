package pmeig.spring.libraries.logger.correlation.http

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.correlation.correlationId

@Configuration
@ConditionalOnClass(RestTemplate::class)
class RestTemplateConfiguration(private val properties: CorrelationProperties): BeanPostProcessor {
  override fun postProcessAfterInitialization(bean: Any, beanName: String)
  = if (bean is RestTemplate) {
    val interceptors = bean.interceptors.toMutableList()
    interceptors.add({
      request, body, execution ->
      request.headers.add(properties.header,  correlationId.toString())
      execution.execute(request, body)
    })
  } else super.postProcessAfterInitialization(bean, beanName)
}