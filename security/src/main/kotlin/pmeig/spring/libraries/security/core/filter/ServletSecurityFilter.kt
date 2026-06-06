package pmeig.spring.libraries.security.core.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pmeig.spring.libraries.security.core.SecurityHandler
import pmeig.spring.libraries.security.core.model.api.ServletAdapter

@Configuration
@ConditionalOnWebApplication(type = SERVLET)
class ServletSecurityFilter(private val securityAuthenticationProviders: List<SecurityAuthenticationProvider>) :
  SecurityHandler {
  override fun handle(http: HttpSecurity): HttpSecurity {
    return super.handle(securityAuthenticationProviders.fold(http) { acc, provider ->
      acc.addFilterAfter({
          request, response, chain -> authenticationFilter(request as HttpServletRequest, response, chain, provider)
      }, UsernamePasswordAuthenticationFilter::class.java)
    })
  }

  private fun authenticationFilter(request: HttpServletRequest, response: ServletResponse, chain: FilterChain, provider: SecurityAuthenticationProvider) {
    var authentication = SecurityContextHolder.getContext().authentication
    authentication = provider.from(
      ServletAdapter(
        ServletServerHttpRequest(request)
      ), authentication
    )
    SecurityContextHolder.getContext().authentication = authentication
    chain.doFilter(request, response)
  }
}