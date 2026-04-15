package pmeig.spring.libraries.test

import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBeanNamesForAnnotation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

@Configuration
class RegistrarTest: BeanFactoryPostProcessor, EnvironmentAware {
  private lateinit var environment: Environment
  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    println(environment)
    beanFactory.getBeanNamesForAnnotation<SpringBootApplication>().forEach {
      println(it)
      println(beanFactory.getBeanDefinition(it).beanClassName)
    }
  }

  override fun setEnvironment(environment: Environment) {
    this.environment = environment
  }


}