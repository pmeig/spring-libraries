package pmeig.spring.libraries.jpa.core.registrar

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBeanNamesForAnnotation
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.boot.SpringBootConfiguration
import org.springframework.cglib.proxy.Proxy
import org.springframework.context.EnvironmentAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.type.filter.AnnotationTypeFilter
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.converter.result.DataConverter
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.executor.DataContextService
import pmeig.spring.libraries.jpa.core.executor.JpaMethodInvoker
import pmeig.spring.libraries.jpa.core.executor.JpaRepositoryHandler
import pmeig.spring.libraries.jpa.core.executor.ObjectMethodInvoker
import java.lang.reflect.ParameterizedType
import kotlin.collections.forEach
import kotlin.reflect.KClass

@Configuration
abstract class DataJpaRepositorySupportRegistrar<T: Any>: EnvironmentAware, BeanFactoryPostProcessor {
  protected lateinit var environment: Environment

  protected abstract fun annotationScan(): KClass<out Annotation>

  override fun setEnvironment(environment: Environment) {
    this.environment = environment
  }

  override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
    val packageName = beanFactory.getBeanNamesForAnnotation<SpringBootConfiguration>().firstOrNull()?.let {
      val className = beanFactory.getBeanDefinition(it).beanClassName ?: ""
      className.substringBeforeLast(".")
    } ?: ""
    val beanDefinitionNames = retrieveRegistrarBeanDefinitionName(beanFactory)

    retrieveRepositoryToRegistrar(packageName).forEach { repositoryBeanDefinition ->
      val repository = Class.forName(repositoryBeanDefinition.beanClassName!!)
      val entityClass = Class.forName(
        (repository.genericInterfaces.find { it is ParameterizedType } as ParameterizedType)
          .actualTypeArguments[0].typeName
      )
      val beanDefinitionBuilders =
        registrarJpaMethodInvoker(beanFactory, repository, entityClass, beanDefinitionNames).toMutableList()
      val converters = registrarDataConverters(beanFactory, repository, entityClass, beanDefinitionNames)
      beanDefinitionBuilders.addAll(converters)
      val handler = createJpaRepositoryHandler(
        beanDefinitionBuilders.map { it.first },
        converters.map { it.first },
        beanDefinitionNames,
        repository
      )
      beanDefinitionBuilders.addAll(
        listOf(
          handler,
          createJpaRepository(handler.first, repository)
        )
      )
      beanFactory as BeanDefinitionRegistry
      beanDefinitionBuilders.forEach { (name, builder) ->
        beanFactory.registerBeanDefinition(
          name,
          builder.getBeanDefinition()
        )
      }
    }
  }

  protected abstract fun registrarJpaMethodInvoker(
    beanFactory: ConfigurableListableBeanFactory,
    repository: Class<*>,
    entityClass: Class<*>,
    registrarBeanDefinitionName: DataRegistrarBeanDefinitionName<T>
  ): List<Pair<String, BeanDefinitionBuilder>>

  @Suppress("unused")
  protected fun registrarDataConverters(
    beanFactory: ConfigurableListableBeanFactory,
    repository: Class<*>,
    entityClass: Class<*>,
    registrarBeanDefinitionName: DataRegistrarBeanDefinitionName<T>
  ): List<Pair<String, BeanDefinitionBuilder>> = listOf()

  private fun createJpaRepositoryHandler(
    beanDefinitionNameJpaMethodInvoker: List<String>,
    beanDefinitionNameDataConverter: List<String>,
    beanDefinitionName: DataRegistrarBeanDefinitionName<T>,
    repository: Class<*>
  ): Pair<String, BeanDefinitionBuilder> {
    val methodInvokers = beanDefinitionName.jpaMethodInvoker(*beanDefinitionNameJpaMethodInvoker.toTypedArray())
    methodInvokers.add(ObjectMethodInvoker(repository))

    val rootBeanDefinition =
      BeanDefinitionBuilder.rootBeanDefinition(JpaRepositoryHandler::class.java)
        .addConstructorArgValue(methodInvokers)
        .addConstructorArgValue(beanDefinitionName.dataConverters(*beanDefinitionNameDataConverter.toTypedArray()))
        .addConstructorArgReference(beanDefinitionName.cacheManagerBeanName)

    val nameBigQueryRepository = repository.typeName + annotationScan().simpleName
    return Pair(nameBigQueryRepository, rootBeanDefinition)
  }

  private fun createJpaRepository(
    name: String,
    repository: Class<*>
  ): Pair<String, BeanDefinitionBuilder> {
    val rootBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(Proxy::class.java)
      .setFactoryMethod("newProxyInstance")
      .addConstructorArgValue(repository.classLoader)
      .addConstructorArgValue(arrayOf(repository))
      .addConstructorArgReference(name)

    return repository.simpleName.replaceFirstChar { it.lowercaseChar() } to rootBeanDefinition
  }

  private fun retrieveRegistrarBeanDefinitionName(beanFactory: ConfigurableListableBeanFactory): DataRegistrarBeanDefinitionName<T> {
    val dataCacheManagerName = beanFactory.getBeanNamesForType(DataCacheManager::class.java).first()
    val entityAnnotationReaderName = beanFactory.getBeanNamesForType(EntityAnnotationReader::class.java).first()
    val dataContextServiceName = beanFactory.getBeanNamesForType(DataContextService::class.java).first()
    val anotherJpaMethodInvoker = beanFactory.getBeanNamesForType(JpaMethodInvoker::class.java).toList()
    val dataConverters = beanFactory.getBeanNamesForType(DataConverter::class.java).toList()

    return DataRegistrarBeanDefinitionName(
      dataCacheManagerName, entityAnnotationReaderName,
      dataContextServiceName, loadBeanDefinitionName(beanFactory), anotherJpaMethodInvoker, dataConverters
    )
  }

  @Suppress("unused")
  protected fun loadBeanDefinitionName(beanFactory: ConfigurableListableBeanFactory): T? {
    return null
  }

  private fun retrieveRepositoryToRegistrar(packageName: String): Set<BeanDefinition> {
    val scanner = object: ClassPathScanningCandidateComponentProvider(false, environment) {
      override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
        return beanDefinition.metadata.isInterface
      }
    }
    scanner.addIncludeFilter(AnnotationTypeFilter(annotationScan().java, true, true))
    return scanner.findCandidateComponents(packageName)
  }
}