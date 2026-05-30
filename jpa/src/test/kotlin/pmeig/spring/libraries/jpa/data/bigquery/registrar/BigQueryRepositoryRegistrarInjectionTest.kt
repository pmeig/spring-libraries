package pmeig.spring.libraries.jpa.data.bigquery.registrar

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cglib.proxy.Proxy
import org.springframework.context.annotation.Bean
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.core.executor.DataContextService
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.shared.TestEntityRepository

@SpringBootConfiguration
class ApplicationTest


@SpringBootTest(
  classes = [
    ApplicationTest::class,
    BigQueryRepositoryRegistrarInjectionTest.TestConfig::class,
    BigQueryRepositoryRegistrar::class
  ]
)
class BigQueryRepositoryRegistrarInjectionTest {

  @Autowired
  lateinit var testEntityRepository: TestEntityRepository


  @Test
  fun `proxy bean should be injected as TestEntityRepository`() {
    assertNotNull(testEntityRepository)
  }

  @Test
  fun `injected bean should be a JDK dynamic proxy`() {
    assertTrue(Proxy.isProxyClass(testEntityRepository::class.java))
  }

  @TestConfiguration
  class TestConfig {

    @Bean
    fun dataCacheManager(): DataCacheManager =
      DataCacheManager()

    @Bean
    fun beanPostProcessor(): BeanFactoryPostProcessor = BeanFactoryPostProcessor {

    }
    @Bean
    fun bigQueryClient(): BigQueryClient = mock(BigQueryClient::class.java)

    @Bean
    fun entityAnnotationReader(dataCacheManager: DataCacheManager): EntityAnnotationReader =
      EntityAnnotationReader(dataCacheManager)

    @Bean
    fun dataContextService(dataCacheManager: DataCacheManager): DataContextService = DataContextService(dataCacheManager)

    @Bean
    fun bigQueryMapperFactory(): BigQueryMapperFactory = mock(BigQueryMapperFactory::class.java)

  }
}