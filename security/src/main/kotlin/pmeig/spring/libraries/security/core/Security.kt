package pmeig.spring.libraries.security.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import pmeig.spring.libraries.security.authentication.core.JwtModule

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SecurityModule::class)
annotation class EnablePmeigSecurity {

}

//@AutoConfiguration
@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig", name = ["security"], havingValue = true, matchIfMissing = true)
@ComponentScan(basePackageClasses = [SecurityModule::class, JwtModule::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSecurity::class)])
class PmeigSecurityAutoConfiguration {
}

@ComponentScan(basePackageClasses = [SecurityModule::class, JwtModule::class], lazyInit = true,
  excludeFilters = [ComponentScan.Filter(EnablePmeigSecurity::class, PmeigSecurityAutoConfiguration::class)])
class SecurityModule {

}