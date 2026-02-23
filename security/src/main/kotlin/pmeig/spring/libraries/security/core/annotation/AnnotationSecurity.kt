package pmeig.spring.libraries.security.core.annotation

import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import pmeig.spring.libraries.security.core.annotation.services.SecurityAnnotationService
import pmeig.spring.libraries.security.core.SecurityHandler
import pmeig.spring.libraries.security.core.models.SecurityAdapter
import pmeig.spring.libraries.security.core.models.createSecurityAdapter

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class AnnotationSecurity(private val securityAnnotationService: SecurityAnnotationService) : SecurityHandler {

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

  @Suppress("DuplicatedCode")
  private fun applySecurity(adapter: SecurityAdapter) {
    securityAnnotationService.findAllSecurityAnnotations().forEach {
      if (it.public) adapter.permitAll(it.path, it.methods)
      else if (it.denied) adapter.denyAll(it.path, it.methods)
      else {
        it.accepted
          .filter { accept ->
            println(accept)
            accept.features.isNotEmpty()
          }
          .forEach { (features, type) ->
            val apply = if (type == PmeigSecurity.Type.AND) adapter::hasAllAuthorities else adapter::hasAnyAuthority
            apply(it.path, it.methods, features.toTypedArray())
          }
        it.rejected
          .filter { reject ->
            println(reject)
            reject.features.isNotEmpty()
          }
          .forEach { (features, type) ->
            val apply = if (type == PmeigSecurity.Type.AND) adapter::hasNoneAuthorities else adapter::hasNotAnyAuthority
            apply(it.path, it.methods, features.toTypedArray())
          }
      }
    }
  }


}