package pmeig.spring.libraries.jpa.core.registrar

import org.springframework.beans.factory.config.RuntimeBeanReference
import org.springframework.beans.factory.support.ManagedList

data class DataRegistrarBeanDefinitionName<T>(
  val cacheManagerBeanName: String,
  val entityAnnotationReaderBeanName: String,
  val dataContextServiceBeanName: String,
  val loadBeanDefinitionName: T?,
  val auditingManagerBeanName: String,
  private val anotherJpaMethodInvoker: List<String>,
  private val dataConvertersBeanName: List<String>,
) {

  fun dataConverters(vararg converters: Any) = toManagedList(dataConvertersBeanName, converters)

  fun jpaMethodInvoker(vararg invokers: Any) = toManagedList(anotherJpaMethodInvoker, invokers)

  private fun toManagedList(defaultBeans: List<String>, concat: Array<out Any>) = ManagedList<Any>().apply {
    addAll(defaultBeans.map { RuntimeBeanReference(it) })
    addAll(concat.map { if (it is String) RuntimeBeanReference(it) else it })
  }
}
