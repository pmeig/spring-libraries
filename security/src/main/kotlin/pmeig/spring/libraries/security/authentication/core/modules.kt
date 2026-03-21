@file:Suppress("unused")

package pmeig.spring.libraries.security.authentication.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import pmeig.spring.libraries.cache.core.model.CacheConfig
import pmeig.spring.libraries.security.authentication.cache.SecurityCache
import pmeig.spring.libraries.security.authentication.jwt.JwtService
import pmeig.spring.libraries.security.authentication.jwt.login.JwtLoginController
import pmeig.spring.libraries.security.authentication.jwt.login.ReactiveJwtLoginController

@ComponentScan(basePackageClasses = [JwtService::class])
@ConditionalOnProperty(prefix = "spring.security.pmeig.auth", name = ["type"], havingValue = "jwt", matchIfMissing = true)
class JwtModule

@Suppress("unused")
@ComponentScan(basePackageClasses = [JwtService::class], excludeFilters = [ComponentScan.Filter(JwtLoginController::class,
  ReactiveJwtLoginController::class)])
class JwtCoreModule

@ComponentScan(basePackageClasses = [SecurityCache::class])
@ConditionalOnClass(CacheConfig::class)
@ConditionalOnProperty(prefix = "spring.security.pmeig.auth", name = ["type"], havingValue = "cache", matchIfMissing = true)
class CacheModule


