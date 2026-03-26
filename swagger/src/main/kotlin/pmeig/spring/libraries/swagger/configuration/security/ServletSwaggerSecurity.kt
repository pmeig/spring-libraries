package pmeig.spring.libraries.swagger.configuration.security

import org.springdoc.core.properties.SpringDocConfigProperties
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(HttpSecurity::class)
class ServletSwaggerSecurity: BeanPostProcessor, ApplicationContextAware {
  private lateinit var applicationContext: ApplicationContext
  override fun setApplicationContext(applicationContext: ApplicationContext) {
    this.applicationContext = applicationContext
  }

  override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
    if (bean is HttpSecurity) {
      val swaggerUiConfigProperties = applicationContext.getBean(SwaggerUiConfigProperties::class.java)
      val springDocConfigProperties = applicationContext.getBean(SpringDocConfigProperties::class.java)
      return bean.authorizeHttpRequests { authorize ->
        authorize.requestMatchers(*arrayOf("/webjars/**", "/swagger-ui/**",
          startedBySlash(swaggerUiConfigProperties.path), startedBySlash("${springDocConfigProperties.apiDocs.path}/**")))
          .permitAll()
      }
    }
    return super.postProcessAfterInitialization(bean, beanName)
  }

  private fun startedBySlash(path: String) = if (path.startsWith("/")) path else "/$path"
}