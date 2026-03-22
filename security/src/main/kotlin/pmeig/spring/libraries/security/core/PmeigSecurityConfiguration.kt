package pmeig.spring.libraries.security.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@ConditionalOnWebApplication(type = SERVLET)
@EnableWebSecurity
class PmeigSecurityConfiguration {

  @Bean
  fun securityConfig(handlers: List<SecurityHandler>, httpSecurity: HttpSecurity): SecurityFilterChain {
    return handlers.fold(httpSecurity) { acc, handler -> handler.handle(acc) }.build()
  }
}


@Configuration
@ConditionalOnWebApplication(type = REACTIVE)
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class PmeigReactiveAnnotation {

  @Bean
  fun securityConfig(handlers: List<SecurityHandler>, http: ServerHttpSecurity): SecurityWebFilterChain? {
    return handlers.fold(http) { acc, handler -> handler.handle(acc) }.build()
  }

}