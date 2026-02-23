package pmeig.spring.libraries.security.authentication.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import pmeig.spring.libraries.security.authentication.jwt.JwtService
import pmeig.spring.libraries.security.authentication.jwt.login.JwtLoginController
import pmeig.spring.libraries.security.authentication.jwt.login.ReactiveJwtLoginController

@ComponentScan(basePackageClasses = [JwtService::class])
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security.auth", name = ["type"], havingValue = "jwt", matchIfMissing = true)
class JwtModule {}

@Suppress("unused")
@ComponentScan(basePackageClasses = [JwtService::class], excludeFilters = [ComponentScan.Filter(JwtLoginController::class,
  ReactiveJwtLoginController::class)])
class JwtCoreModule {}
