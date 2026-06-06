package pmeig.spring.libraries.security.core.authorization

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import pmeig.spring.libraries.security.core.SecurityHandler
import pmeig.spring.libraries.security.core.annotation.PmeigSecurity
import pmeig.spring.libraries.security.core.authorization.provider.SecurityAuthorizationProvider
import pmeig.spring.libraries.security.core.model.SecurityAdapter
import pmeig.spring.libraries.security.core.model.createSecurityAdapter

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
      val paths = it.path.map { path -> if (path.startsWith("/")) path else "/$path" }
      if (it.public) adapter.permitAll(paths, it.methods)
      else if (it.denied) adapter.denyAll(paths, it.methods)
      else {
        securityMethods[it.contain]?.let { methods ->
          methods[it.type]?.invoke(adapter, paths, it.methods, it.features.toTypedArray())
        }
      }
    }
  }
}