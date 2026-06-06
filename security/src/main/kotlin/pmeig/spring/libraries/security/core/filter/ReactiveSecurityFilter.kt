package pmeig.spring.libraries.security.core.filter

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.web.server.WebFilter
import pmeig.spring.libraries.security.core.SecurityHandler
import pmeig.spring.libraries.security.core.model.api.ReactiveAdapter
import java.util.LinkedList

@Configuration
@ConditionalOnWebApplication(type = REACTIVE)
class ReactiveSecurityFilter(private val securityAuthenticationProvider: List<SecurityAuthenticationProvider>) :
  SecurityHandler {

  override fun handle(http: ServerHttpSecurity): ServerHttpSecurity {
    val filters = LinkedList(securityAuthenticationProvider)
    val firstFilter = filters.poll()
    val httpSecurity = firstFilter?.let {
      filters.fold(
        http.addFilterAt(filter(it), SecurityWebFiltersOrder.AUTHENTICATION)
      ) { acc, provider ->
        acc.addFilterAfter(filter(provider), SecurityWebFiltersOrder.AUTHENTICATION)
      }
    } ?: http
    return super.handle(httpSecurity)
  }

  private fun filter(provider: SecurityAuthenticationProvider) = WebFilter { exchange, chain ->
    val next = { chain.filter(exchange) }
    val request = ReactiveAdapter(exchange.request)
    ReactiveSecurityContextHolder.getContext().flatMap { context ->
      provider.from(request, context.authentication)?.let {
        next().contextWrite(ReactiveSecurityContextHolder.withAuthentication(it))
      } ?: next()
    }
  }
}