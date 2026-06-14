package pmeig.spring.libraries.jpa.data.bigquery.registrar.repository.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.jpa.repository.Query
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.executor.DataContextService
import pmeig.spring.libraries.jpa.data.bigquery.client.BigQueryClient
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.UnaryOperator
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryJpaBigQueryRepositoryTest {

  private val client = mock<BigQueryClient>()
  private val cacheManager = mock<DataCacheManager>()
  private val dataContextService = DataContextService(cacheManager)
  private val bigQueryMapperFactory = BigQueryMapperFactory(ObjectMapper(), cacheManager)
  private val tableResult = mock<TableResult>()
  private val result = listOf(
    FieldValueList.of(
      listOf(FieldValue.of(FieldValue.Attribute.PRIMITIVE, "test"))
    )
  )

  private val repository = QueryJpaBigQueryRepository(
    client,
    cacheManager,
    dataContextService,
    bigQueryMapperFactory
  )

  private interface SampleRepository {
    @Query(
      "SELECT name FROM sample WHERE id = ?1 AND status = ?2"
    )
    fun queryWithPositional(id: Long, status: String): String

    @Query(
      "SELECT name FROM sample WHERE id = :id"
    )
    fun queryWithNamed(id: Long): String

    @Query(
      "SELECT name FROM sample WHERE status = ?1"
    )
    fun queryWithConfigurator(
      status: String,
      configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder
    ): String

    @Query(
      "SELECT name FROM sample WHERE status = ?1"
    )
    fun queryWithConsumer(
      status: String,
      configurator: Consumer<QueryJobConfiguration.Builder>
    ): String

    @Query(
      "SELECT name FROM sample WHERE status = ?1"
    )
    fun queryWithFunction(
      status: String,
      configurator: Function<QueryJobConfiguration.Builder, QueryJobConfiguration.Builder>
    ): String

    @Query(
      "SELECT name FROM sample WHERE status = ?1"
    )
    fun queryWithUnary(
      status: String,
      configurator: UnaryOperator<QueryJobConfiguration.Builder>
    ): String

    fun noQuery(): String
  }

  private fun method(name: String, vararg parameterTypes: Class<*>) =
    SampleRepository::class.java.getDeclaredMethod(name, *parameterTypes)

  @Nested
  inner class WithAnnotation {

    @Test
    fun `returns not executed result when DataContextService returns null query`() {
      val m = method("noQuery")
      val result = repository.invokeMethod(m, String::class.java, arrayOf(1L))

      assertFalse(result.executed)
      assertEquals(null, result.result)
    }

    @Test
    fun `executes query when sql is present`() {
      val m = method("queryWithNamed", Long::class.java)
      val expected = "toto"

      whenever(tableResult.values).thenReturn(result)
      whenever(client.trySingle(eq(String::class), any(), any()))
        .thenReturn(expected)

      val result = repository.invokeMethod(m, String::class.java, arrayOf(1L))

      assertTrue(result.executed)
      assertEquals(expected, result.result)
    }
  }

  @Nested
  inner class CustomConfiguratorByMethod {
    @Test
    fun `supports Consumer configurator parameter`() {
      val m = method(
        "queryWithConsumer",
        String::class.java,
        Consumer::class.java
      )
      val configuratorCaptor = argumentCaptor<(QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder>()

      whenever(
        client.trySingle(
          eq(String::class), any(), configuratorCaptor.capture()
        )
      )
        .thenReturn("result")

      var consumerCalled = false

      val consumer = Consumer<QueryJobConfiguration.Builder> {
        consumerCalled = true
      }
      val result = repository.invokeMethod(m, String::class.java, arrayOf("ACTIVE", consumer))

      configuratorCaptor.firstValue(QueryJobConfiguration.newBuilder("SELECT 1"))

      assertTrue(result.executed)
      assertEquals("result", result.result)
      assertTrue(consumerCalled)
    }

    @Test
    fun `supports Function configurator parameter`() {
      val m = method(
        "queryWithFunction",
        String::class.java,
        Function::class.java
      )

      val configuratorCaptor = argumentCaptor<(QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder>()

      whenever(
        client.trySingle(
          eq(String::class), any(), configuratorCaptor.capture()
        )
      )
        .thenReturn("result")

      var consumerCalled = false

      val consumer = UnaryOperator<QueryJobConfiguration.Builder> {
        consumerCalled = true
        it
      }
      val result = repository.invokeMethod(m, String::class.java, arrayOf("ACTIVE", consumer))

      configuratorCaptor.firstValue(QueryJobConfiguration.newBuilder("SELECT 1"))

      assertTrue(result.executed)
      assertEquals("result", result.result)
      assertTrue(consumerCalled)
    }
  }
}
