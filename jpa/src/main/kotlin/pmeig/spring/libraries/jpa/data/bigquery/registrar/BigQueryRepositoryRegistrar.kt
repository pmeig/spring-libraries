package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBeanNamesForAnnotation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AnnotationTypeFilter
import pmeig.spring.libraries.jpa.data.bigquery.annotation.BigQueryRepository

@Configuration
class BigQueryRepositoryRegistrar: BeanFactoryPostProcessor, EnvironmentAware {
  private lateinit var environment: Environment

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    val packageName = beanFactory.getBeanNamesForAnnotation<SpringBootApplication>().firstOrNull()?.let {
      val classname = beanFactory.getBeanDefinition(it).beanClassName ?: ""
      classname.substringBeforeLast(".")
    } ?: ""
    val bigQueryRepositories = retrieveBigQueryRepositories(packageName)
    bigQueryRepositories.forEach { beanDefinition ->

    }

  }

  private fun retrieveBigQueryRepositories(packageName: String): Set<BeanDefinition> {
    val scanner = ClassPathScanningCandidateComponentProvider(true, environment)
    scanner.addIncludeFilter(AnnotationTypeFilter(BigQueryRepository::class.java))
    return scanner.findCandidateComponents(packageName)
  }

  override fun setEnvironment(environment: Environment) {
    this.environment = environment
  }
}