package pmeig.spring.libraries.security.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import pmeig.spring.libraries.security.authentication.core.JwtModule

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SecurityModule::class)
annotation class EnablePmeigSecurity {

}

//@AutoConfiguration
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackageClasses = [PmeigSecurityAutoConfiguration::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSecurity::class)])
@Import(JwtModule::class)
class PmeigSecurityAutoConfiguration {
}

@ComponentScan(basePackageClasses = [SecurityModule::class], lazyInit = true,
  excludeFilters = [ComponentScan.Filter(EnablePmeigSecurity::class)])
@Import(JwtModule::class)
class SecurityModule {

}