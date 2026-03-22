package pmeig.spring.libraries.security.core

import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity

interface SecurityHandler {
  fun handle(http: HttpSecurity): HttpSecurity {
    return http
  }
  fun handle(http: ServerHttpSecurity): ServerHttpSecurity {
    return http
  }
}