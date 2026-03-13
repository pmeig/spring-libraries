package pmeig.spring.libraries.logger.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.ComponentScan
import pmeig.spring.libraries.logger.beans.YamlConfigurer
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.integration.IntegrationBean

@Suppress("unused")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ComponentScan(basePackageClasses = [IntegrationBean::class, YamlConfigurer::class, CorrelationProperties::class])
annotation class EnablePmeigLogger

@ComponentScan(basePackageClasses = [IntegrationBean::class, YamlConfigurer::class, CorrelationProperties::class])
@ConditionalOnProperty(prefix = "spring.plugins.pmeig.logger", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class LoggerModule {}