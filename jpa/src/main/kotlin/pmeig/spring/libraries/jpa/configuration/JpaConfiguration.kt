package pmeig.spring.libraries.jpa.configuration

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.REACTIVE
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type.SERVLET
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.domain.ReactiveAuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolder
import java.util.Optional

@Configuration
@EnableJpaAuditing
class JpaConfiguration {

  @Suppress("UNCHECKED_CAST", "UNNECESSARY_NOT_NULL_ASSERTION")
  @Bean
  @ConditionalOnWebApplication(type = SERVLET)
  @ConditionalOnClass(SecurityContextHolder::class)
  @ConditionalOnMissingBean(AuditorAware::class)
  fun defaultAuditorAware(): AuditorAware<String> = AuditorAware {
    Optional.ofNullable(SecurityContextHolder.getContext()).map {
      context ->
      context.authentication
    }.map { authentication -> authentication!!.name } as Optional<String>
  }

  @Bean
  @ConditionalOnWebApplication(type = REACTIVE)
  @ConditionalOnClass(ReactiveSecurityContextHolder::class)
  @ConditionalOnMissingBean(ReactiveAuditorAware::class)
  fun defaultReactiveAuditor(): ReactiveAuditorAware<String> = ReactiveAuditorAware {
    ReactiveSecurityContextHolder.getContext().defaultIfEmpty(SecurityContextHolder.createEmptyContext())
      .mapNotNull { it.authentication }
      .mapNotNull { authentication -> authentication.name }
  }
}