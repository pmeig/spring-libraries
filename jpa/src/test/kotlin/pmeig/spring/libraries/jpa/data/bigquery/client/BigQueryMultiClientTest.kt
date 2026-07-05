package pmeig.spring.libraries.jpa.data.bigquery.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQueryMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.shared.expected.entity_test_expected
import pmeig.spring.libraries.jpa.shared.helper.entityTest_result
import pmeig.spring.libraries.jpa.shared.helper.entityTest_schema
import pmeig.spring.libraries.jpa.shared.helper.pageable_result
import pmeig.spring.libraries.jpa.shared.helper.pageable_schema
import pmeig.spring.libraries.jpa.shared.helper.single_field
import pmeig.spring.libraries.jpa.shared.helper.single_result
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BigQueryMultiClientTest {

  private val bigQuery = mock<BigQuery>()
  private val jobIdBuilder = mock<JobId.Builder>()
  private val properties = mock<GcpBigQueryProperties>()
  private val cacheManager = mock<DataCacheManager>()
  private val entityAnnotationReader = EntityAnnotationReader(cacheManager)
  private val sqlMapper = BigQuerySqlMapper()
  private val mapperFactory = BigQueryMapperFactory(ObjectMapper(), cacheManager)

  private val client = BigQueryClient(
    bigQuery,
    jobIdBuilder,
    properties,
    entityAnnotationReader,
    sqlMapper,
    cacheManager,
    mapperFactory
  )

  @BeforeEach
  fun setUp() {
    whenever(properties.projectId).thenReturn("default-project")
    whenever(properties.datasetName).thenReturn("default-dataset")
    whenever(jobIdBuilder.setRandomJob()).thenReturn(jobIdBuilder)
    whenever(jobIdBuilder.build()).thenReturn(mock())
  }

  @Nested
  inner class Multiple {

    private val result = mock<TableResult>()

    @BeforeEach
    fun prepare() {
      whenever(bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)
    }

    @Test
    fun `multiple maps first field of each row`() {
      whenever(result.iterateAll()).thenReturn(
        listOf(
          FieldValueList.of(listOf(single_result()), single_field()),
          FieldValueList.of(listOf(single_result()), single_field())
        )
      )

      val actual = client.multiple(Long::class, "select 1")

      assertEquals(2, actual.size)
      assertEquals(1L, actual[0])
      assertEquals(1L, actual[1])
      verify(bigQuery).query(any<QueryJobConfiguration>(), any<JobId>())
      verify(jobIdBuilder).setRandomJob()
      verify(jobIdBuilder).build()
    }

    @Test
    fun `multiple returns null when first field is null`() {
      val fieldValue = mock<FieldValue>()
      whenever(fieldValue.isNull).thenReturn(true)
      whenever(result.iterateAll()).thenReturn(
        listOf(FieldValueList.of(listOf(fieldValue), single_field()))
      )

      val actual = client.multiple(Long::class, "select 1")

      assertEquals(listOf(null), actual)
    }

    @Test
    fun `tryMultiple returns empty list when query returns null`() {
      whenever(bigQuery.create(any<JobInfo>())).thenThrow(RuntimeException("boom"))

      val actual = client.tryMultiple(Long::class, "select 1")

      assertTrue(actual.isEmpty())
    }
  }

  @Nested
  inner class Records {

    private val result = mock<TableResult>()

    @BeforeEach
    fun prepare() {
      whenever(bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)
    }

    @Test
    fun `records maps rows using metadata factory`() {
      whenever(result.schema).thenReturn(entityTest_schema())
      whenever(result.iterateAll()).thenReturn(
        listOf(entityTest_result()))

      val mappedField = mock<BigQueryMapper<Any>>()
      whenever(mappedField.map(any())).thenReturn("mapped-value")
      val actual = client.records("select 1", mapOf("x" to mock()))

      assertEquals(1, actual.size)
      assertEquals(1L, actual.first()["id"])
    }

    @Test
    fun `tryRecords returns empty list when query fails`() {
      whenever(bigQuery.create(any<JobInfo>())).thenThrow(RuntimeException("boom"))

      val actual = client.tryRecords("select 1")

      assertTrue(actual.isEmpty())
    }
  }

  @Nested
  inner class Entities {

    private val result = mock<TableResult>()

    @BeforeEach
    fun prepare() {
      whenever(bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)
      whenever(result.schema).thenReturn(entityTest_schema())
      whenever(result.iterateAll()).thenReturn(
        listOf(entityTest_result()))
    }

    @Test
    fun `entities(Class) delegates to kotlin overload`() {

      val actual = client.entities(EntityTest::class.java, "select 1")

      assertEquals(1, actual.size)
      assertIs<EntityTest>(actual.first())

      val expected = entity_test_expected()
      assertEquals(expected, actual.first())
    }

    @Test
    fun `tryEntities returns empty list when query fails`() {
      whenever(bigQuery.create(any<JobInfo>())).thenThrow(RuntimeException("boom"))

      val actual = client.tryEntities(EntityTest::class, "select 1")

      assertTrue(actual.isEmpty())
    }

    @Test
    fun `entities pageable returns page`() {
      val pageable = PageRequest.of(0, 10)
      whenever(result.iterateAll()).thenReturn(
        listOf(pageable_result())
      )

      whenever(result.schema).thenReturn(
        pageable_schema()
      )

      val actual = client.entities(EntityTest::class, pageable, "select 1")

      assertEquals(10, actual.totalElements)
      assertEquals(0, actual.number)
      assertEquals(1, actual.numberOfElements)
    }
  }
}
