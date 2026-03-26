package pmeig.spring.libraries.swagger

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(SwaggerModule::class)
annotation class EnablePmeigSwagger

@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig", name = ["swagger"], havingValue = true, matchIfMissing = true)
@ComponentScan(basePackageClasses = [SwaggerModule::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSwagger::class)])
class SwaggerAutoImport {
}

@ComponentScan(basePackageClasses = [SwaggerModule::class], excludeFilters = [ComponentScan.Filter(EnablePmeigSwagger::class,
  SwaggerAutoImport::class)])
class SwaggerModule {
}