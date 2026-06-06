package pmeig.spring.libraries.logger.core

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.context.annotation.ComponentScan
import pmeig.spring.libraries.logger.beans.YamlConfiguration
import pmeig.spring.libraries.logger.correlation.CorrelationProperties
import pmeig.spring.libraries.logger.integration.IntegrationBean

@Suppress("unused")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ComponentScan(basePackageClasses = [IntegrationBean::class, YamlConfiguration::class, CorrelationProperties::class])
annotation class EnablePmeigLogger

@ComponentScan(basePackageClasses = [IntegrationBean::class, YamlConfiguration::class, CorrelationProperties::class])
@ConditionalOnBooleanProperty(prefix = "spring.plugins.pmeig", name = ["logger"], havingValue = true, matchIfMissing = true)
class LoggerModule {}