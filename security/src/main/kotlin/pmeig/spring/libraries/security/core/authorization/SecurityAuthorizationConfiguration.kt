package pmeig.spring.libraries.security.core.authorization

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import pmeig.spring.libraries.security.core.SecurityHandler
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.models.SecurityAdapter
import pmeig.spring.libraries.security.core.models.createSecurityAdapter

private val securityMethods = mapOf(Pair(true,
  mapOf(Pair(PmeigSecurity.Type.AND, SecurityAdapter::hasAllAuthorities), Pair(PmeigSecurity.Type.OR, SecurityAdapter::hasAnyAuthority))
), Pair(false,
  mapOf(Pair(PmeigSecurity.Type.AND, SecurityAdapter::hasNoneAuthorities), Pair(PmeigSecurity.Type.AND, SecurityAdapter::hasNotAnyAuthority))
))

@Configuration
class SecurityAuthorizationConfiguration(
  private val authorizationProviders: List<SecurityAuthorizationProvider>
): SecurityHandler {

  override fun handle(http: HttpSecurity): HttpSecurity {
    return http.authorizeHttpRequests {
      applySecurity(createSecurityAdapter(it))
      it.anyRequest().authenticated()
    }
  }

  override fun handle(http: ServerHttpSecurity): ServerHttpSecurity {
    return http.authorizeExchange {
      applySecurity(createSecurityAdapter(it))
      it.anyExchange().authenticated()
    }
  }

  private fun applySecurity(adapter: SecurityAdapter) {
    authorizationProviders.flatMap { it.get() }.forEach {
      if (it.public) adapter.permitAll(it.path, it.methods)
      else if (it.denied) adapter.denyAll(it.path, it.methods)
      else {
        securityMethods[it.contain]?.let { methods ->
          methods[it.type]?.invoke(adapter, it.path, it.methods, it.features.toTypedArray())
        }
      }
    }
  }
}