package pmeig.spring.libraries.security.core.annotation

import org.springframework.context.annotation.Configuration
import pmeig.spring.libraries.security.core.annotation.services.SecurityAnnotationService
import pmeig.spring.libraries.security.core.authorization.SecurityAuthorizationProvider

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@Configuration
class AnnotationSecurity(private val securityAnnotationService: SecurityAnnotationService) : SecurityAuthorizationProvider {
  override fun get() = securityAnnotationService.findAllSecurityAnnotations()
}