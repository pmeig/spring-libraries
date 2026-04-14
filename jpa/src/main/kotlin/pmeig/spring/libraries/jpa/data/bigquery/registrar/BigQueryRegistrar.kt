package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanNameGenerator
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.type.AnnotationMetadata

internal class BigQueryRegistrar: ImportBeanDefinitionRegistrar {
  override fun registerBeanDefinitions(
    importingClassMetadata: AnnotationMetadata,
    registry: BeanDefinitionRegistry,
    importBeanNameGenerator: BeanNameGenerator
  ) {
    super.registerBeanDefinitions(importingClassMetadata, registry, importBeanNameGenerator)
  }
}