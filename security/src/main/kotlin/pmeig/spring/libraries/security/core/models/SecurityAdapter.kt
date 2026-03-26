package pmeig.spring.libraries.security.core.models

import org.springframework.security.authorization.AuthorityAuthorizationDecision
import org.springframework.security.authorization.AuthorityReactiveAuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.authorization.ReactiveAuthorizationManager
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.server.authorization.AuthorizationContext
import org.springframework.web.bind.annotation.RequestMethod
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface SecurityAdapter {
  fun permitAll(path: List<String>, methods: List<RequestMethod>)
  fun denyAll(path: List<String>, methods: List<RequestMethod>)
  fun hasAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String)
  fun hasNotAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String)
  fun hasAllAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String)
  fun hasNoneAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String)
}

internal class ServletSecurityAdapter(private val configurer: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry) :
  SecurityAdapter {

  override fun permitAll(path: List<String>, methods: List<RequestMethod>) {
    applyAuthority(path, methods) { it.permitAll() }
  }

  override fun denyAll(path: List<String>, methods: List<RequestMethod>) {
    applyAuthority(path, methods) { it.denyAll() }
  }

  override fun hasAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.hasAnyAuthority(*authorities) }
  }

  override fun hasNotAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.not().hasAnyAuthority(*authorities) }
  }

  override fun hasAllAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.hasAllAuthorities(*authorities) }
  }

  override fun hasNoneAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.not().hasAllAuthorities(*authorities) }
  }

  private fun applyAuthority(
    path: List<String>,
    methods: List<RequestMethod>,
    function: (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl) -> Unit
  ) {
    if (methods.isEmpty()) {
      return function(configurer.requestMatchers(*path.toTypedArray()))
    }
    methods.forEach { function(configurer.requestMatchers(it.asHttpMethod(), *path.toTypedArray())) }
  }
}

internal class AuthorityReactiveAllAuthorizationManager(private val not: Boolean, vararg authorities: String) :
  ReactiveAuthorizationManager<AuthorizationContext> {
  private val authorities = authorities.map { SimpleGrantedAuthority(it) }
  private val managers =
    Flux.fromIterable(authorities.map { AuthorityReactiveAuthorizationManager.hasAuthority<AuthorizationContext>(it) })

  override fun authorize(
    authentication: Mono<Authentication>,
    `object`: AuthorizationContext
  ): Mono<AuthorizationResult> {
    return managers.flatMap {
      it.authorize(authentication, `object`)
        .flux()
    }
      .all { it.isGranted }
      .map { AuthorityAuthorizationDecision(if (not) !it else it, authorities) }
  }
}

internal class ReactiveSecurityAdapter(private val configurer: ServerHttpSecurity.AuthorizeExchangeSpec) :
  SecurityAdapter {

  override fun permitAll(path: List<String>, methods: List<RequestMethod>) {
    applyAuthority(path, methods) { it.permitAll() }
  }

  override fun denyAll(path: List<String>, methods: List<RequestMethod>) {
    applyAuthority(path, methods) { it.denyAll() }
  }

  override fun hasAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.hasAnyAuthority(*authorities) }
  }

  override fun hasNotAnyAuthority(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) {
      it.access { authentication, _ ->
        authentication.map { auth ->
          auth.authorities.map { authorityAccessor -> authorityAccessor.authority }
            .any { authority -> authority in authorities }
        }
          .map { isGranted -> AuthorityAuthorizationDecision(!isGranted, listOf()) }
      }
    }
  }

  override fun hasAllAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.access(AuthorityReactiveAllAuthorizationManager(false, *authorities)) }
  }

  override fun hasNoneAuthorities(path: List<String>, methods: List<RequestMethod>, vararg authorities: String) {
    applyAuthority(path, methods) { it.access(AuthorityReactiveAllAuthorizationManager(true, *authorities)) }
  }


  private fun applyAuthority(
    path: List<String>,
    methods: List<RequestMethod>,
    function: (ServerHttpSecurity.AuthorizeExchangeSpec.Access) -> Unit
  ) {
    if (methods.isEmpty()) {
      return function(configurer.pathMatchers(*path.toTypedArray()))
    }
    methods.forEach { function(configurer.pathMatchers(it.asHttpMethod(), *path.toTypedArray())) }
  }
}


fun createSecurityAdapter(configurer: AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry): SecurityAdapter =
  ServletSecurityAdapter(configurer)

fun createSecurityAdapter(configurer: ServerHttpSecurity.AuthorizeExchangeSpec): SecurityAdapter =
  ReactiveSecurityAdapter(configurer)