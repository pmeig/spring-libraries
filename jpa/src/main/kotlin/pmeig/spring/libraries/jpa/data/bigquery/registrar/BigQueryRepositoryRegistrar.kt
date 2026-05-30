package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import pmeig.spring.libraries.jpa.core.registrar.DataJpaRepositorySupportRegistrar
import pmeig.spring.libraries.jpa.core.registrar.DataRegistrarBeanDefinitionName
import pmeig.spring.libraries.jpa.data.bigquery.annotation.BigQueryRepository
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.MethodNameBigQueryJpaRepository
import pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.SimpleJpaBigQueryRepository
import pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.query.QueryJpaBigQueryRepository

private const val SUFFIX_SIMPLE_JPA = "SimpleJpaBigQueryRepository"
private const val SUFFIX_QUERY = "QueryJpaBigQueryRepository"
private const val SUFFIX_METHOD = "MethodNameBigQueryJpaRepository"

class BigQueryRepositoryRegistrar: DataJpaRepositorySupportRegistrar<BigQueryBeanDefinitionRequired>() {

  override fun loadBeanDefinitionName(beanFactory: ConfigurableListableBeanFactory): BigQueryBeanDefinitionRequired? {
    return BigQueryBeanDefinitionRequired(
      beanFactory.getBeanNamesForType(BigQueryClient::class.java).first(),
      beanFactory.getBeanNamesForType(BigQueryMapperFactory::class.java).first()
    )
  }

  override fun annotationScan() = BigQueryRepository::class

  override fun registrarJpaMethodInvoker(
    beanFactory: ConfigurableListableBeanFactory,
    repository: Class<*>,
    entityClass: Class<*>,
    registrarBeanDefinitionName: DataRegistrarBeanDefinitionName<BigQueryBeanDefinitionRequired>
  ): List<Pair<String, BeanDefinitionBuilder>> = mutableListOf(
    createSimpleJpaBigQueryRepositoryBeanDefinition(
      registrarBeanDefinitionName,
      entityClass,
      repository.typeName
    ),
    createQueryBigQueryBeanDefinition(
      registrarBeanDefinitionName,
      repository.typeName
    )
  ).apply {
    add(createMethodBigQueryBeanDefinition(
      get(0).first,
      registrarBeanDefinitionName.dataContextServiceBeanName,
      repository.typeName
    ))
  }

  private fun createMethodBigQueryBeanDefinition(
    nameSimpleJpaRepository: String,
    dataContextServiceName: String,
    repositoryName: String
  ): Pair<String, BeanDefinitionBuilder> {
    val methodNameInvoker = BeanDefinitionBuilder.rootBeanDefinition(MethodNameBigQueryJpaRepository::class.java)
      .addConstructorArgReference(nameSimpleJpaRepository)
      .addConstructorArgReference(dataContextServiceName)

    val nameMethodNameInvoker = repositoryName + SUFFIX_METHOD
    return Pair(nameMethodNameInvoker, methodNameInvoker)
  }

  private fun createQueryBigQueryBeanDefinition(
    registrarBeanDefinitionName: DataRegistrarBeanDefinitionName<BigQueryBeanDefinitionRequired>,
    repositoryName: String
  ): Pair<String, BeanDefinitionBuilder> {
    val queryJpaMethodInvoker = BeanDefinitionBuilder.rootBeanDefinition(QueryJpaBigQueryRepository::class.java)
      .addConstructorArgReference(registrarBeanDefinitionName.loadBeanDefinitionName!!.client)
      .addConstructorArgReference(registrarBeanDefinitionName.cacheManagerBeanName)
      .addConstructorArgReference(registrarBeanDefinitionName.dataContextServiceBeanName)
      .addConstructorArgReference(registrarBeanDefinitionName.loadBeanDefinitionName.mapper)

    val nameQueryJpaMethodInvoker = repositoryName + SUFFIX_QUERY
    return Pair(nameQueryJpaMethodInvoker, queryJpaMethodInvoker)
  }

  private fun createSimpleJpaBigQueryRepositoryBeanDefinition(
    beanDefinitionsName: DataRegistrarBeanDefinitionName<BigQueryBeanDefinitionRequired>,
    entityClass: Class<*>,
    repositoryName: String
  ): Pair<String, BeanDefinitionBuilder> {
    val jpaRepositoryMethodInvoker = BeanDefinitionBuilder.rootBeanDefinition(SimpleJpaBigQueryRepository::class.java)
      .addConstructorArgReference(beanDefinitionsName.loadBeanDefinitionName!!.client)
      .addConstructorArgReference(beanDefinitionsName.cacheManagerBeanName)
      .addConstructorArgReference(beanDefinitionsName.entityAnnotationReaderBeanName)
      .addConstructorArgValue(entityClass)
    val nameJpaRepositoryMethodInvoker = repositoryName + SUFFIX_SIMPLE_JPA
    return Pair(nameJpaRepositoryMethodInvoker, jpaRepositoryMethodInvoker)
  }
}