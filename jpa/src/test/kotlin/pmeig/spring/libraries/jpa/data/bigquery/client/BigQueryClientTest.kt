package pmeig.spring.libraries.jpa.data.bigquery.client

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.Table
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TableResult
import com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryProperties
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import pmeig.spring.libraries.jpa.core.cache.DataCacheManager
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.shared.helper.createField
import pmeig.spring.libraries.jpa.shared.helper.entityTest_fields
import pmeig.spring.libraries.jpa.shared.helper.entityTest_result
import pmeig.spring.libraries.jpa.shared.helper.single_field
import pmeig.spring.libraries.jpa.shared.helper.single_result
import pmeig.spring.libraries.jpa.shared.model.EntityTest
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BigQueryClientTest {

  private val properties = mock<GcpBigQueryProperties>()
  private val clientDependencies = ClientDependencies()

  private val client = BigQueryClient(
    clientDependencies.bigQuery,
    clientDependencies.jobIdBuilder,
    properties,
    clientDependencies.entityAnnotationReader,
    clientDependencies.bigQuerySqlMapper,
    clientDependencies.cacheManager,
    clientDependencies.mapperFactory
  )

  private class ClientDependencies {
    val bigQuery = mock<BigQuery>()
    val jobIdBuilder = mock<JobId.Builder>()
    val bigQuerySqlMapper = BigQuerySqlMapper()
    val cacheManager = mock<DataCacheManager>()
    val entityAnnotationReader = EntityAnnotationReader(cacheManager)
    val mapperFactory = mock<BigQueryMapperFactory>()
  }

  @BeforeEach
  fun setUp() {
    whenever(properties.projectId).thenReturn("default-project")
    whenever(properties.datasetName).thenReturn("default-dataset")
    whenever(clientDependencies.jobIdBuilder.setRandomJob()).thenReturn(clientDependencies.jobIdBuilder)
    whenever(clientDependencies.jobIdBuilder.build()).thenReturn(mock())
  }

  @Nested
  inner class TableOperations {

    private val table = mock<Table>()
    private val definition = mock<TableDefinition>()
    private val created = mock<Table>()

    @Test
    fun `createTableId uses defaults when dataset and project are empty`() {
      val tableId = client.createTableId("table")
      assertEquals("default-project", tableId.project)
      assertEquals("default-dataset", tableId.dataset)
      assertEquals("table", tableId.table)
    }

    @Test
    fun `createTableId uses provided dataset and project`() {
      val tableId = client.createTableId("table", "dataset", "project")
      assertEquals("project", tableId.project)
      assertEquals("dataset", tableId.dataset)
      assertEquals("table", tableId.table)
    }

    @Test
    fun `getTable delegates to BigQuery`() {
      val tableId = TableId.of("project", "dataset", "table")
      whenever(clientDependencies.bigQuery.getTable(tableId)).thenReturn(table)
      whenever(table.getDefinition<TableDefinition>()).thenReturn(definition)

      assertEquals(definition, client.getTable(tableId))
      verify(clientDependencies.bigQuery).getTable(tableId)
    }

    @Test
    fun `exists delegates to BigQuery`() {
      val tableId = TableId.of("project", "dataset", "table")
      whenever(clientDependencies.bigQuery.getTable(tableId)).thenReturn(table)
      whenever(table.exists()).thenReturn(true)

      assertTrue(client.exists(tableId))
      verify(clientDependencies.bigQuery).getTable(tableId)
    }

    @Test
    fun `drop delegates to BigQuery`() {
      val tableId = TableId.of("project", "dataset", "table")
      whenever(clientDependencies.bigQuery.delete(tableId)).thenReturn(true)

      assertTrue(client.drop(tableId))
      verify(clientDependencies.bigQuery).delete(tableId)
    }

    @Test
    fun `createTable builds table info with expiration`() {
      val schema = Schema.of(Field.of("name", StandardSQLTypeName.STRING))
      val tableId = TableId.of("project", "dataset", "table")
      whenever(clientDependencies.bigQuery.create(any<TableInfo>())).thenReturn(created)
      whenever(created.exists()).thenReturn(true)

      assertTrue(client.createTable(schema, tableId, Duration.ofMinutes(10)))

      val tableInfoCaptor = argumentCaptor<TableInfo>()
      verify(clientDependencies.bigQuery).create(tableInfoCaptor.capture())
      val createdTableInfo = tableInfoCaptor.firstValue
      assertEquals(tableId, createdTableInfo.tableId)
      assertTrue(createdTableInfo.getDefinition<TableDefinition>() is StandardTableDefinition)
    }
  }

  @Nested
  inner class QueryExecution {

    private val result = mock<TableResult>()

    @Test
    fun `query sets allowLargeResults and uses random job id`() {
      whenever(clientDependencies.bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)

      val actual = client.query("select 1")

      assertEquals(result, actual)
      verify(clientDependencies.bigQuery).query(any<QueryJobConfiguration>(), any<JobId>())
      verify(clientDependencies.jobIdBuilder).setRandomJob()
      verify(clientDependencies.jobIdBuilder).build()
    }

    @Test
    fun `tryQuery returns null on exception`() {
      whenever(clientDependencies.bigQuery.create(any<JobInfo>())).thenThrow(RuntimeException("boom"))

      assertNull(client.tryQuery("select 1"))
      verify(clientDependencies.bigQuery).create(any<JobInfo>())
    }

    @Test
    fun `tryQuery executes dry run before real query`() {
      whenever(clientDependencies.bigQuery.create(any<JobInfo>())).thenReturn(mock())
      whenever(clientDependencies.bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)

      val actual = client.tryQuery("select 1")

      assertEquals(result, actual)
      verify(clientDependencies.bigQuery).create(any<JobInfo>())
      verify(clientDependencies.bigQuery).query(any<QueryJobConfiguration>(), any<JobId>())
    }

    @Test
    fun `query use custom configurator`() {
      val configuratorCatcher = ArgumentCaptor.forClass(QueryJobConfiguration::class.java)
      whenever(clientDependencies.bigQuery.query(configuratorCatcher.capture(), any<JobId>()))
        .thenReturn(result)

      assertSame(result, client.query("select 1") { it.setQuery("SELECT 2") })
      assertEquals("SELECT 2", configuratorCatcher.firstValue.query)
    }

    @Test
    fun `tryQuery use custom configurator`() {
      val configuratorCatcher = ArgumentCaptor.forClass(QueryJobConfiguration::class.java)
      whenever(clientDependencies.bigQuery.query(configuratorCatcher.capture(), any<JobId>()))
        .thenReturn(result)

      assertSame(result, client.tryQuery("select 1") { it.setQuery("SELECT 2") })
      assertEquals("SELECT 2", configuratorCatcher.firstValue.query)
    }
  }

  @Nested
  inner class BatchHelpers {

    private val result = mock<TableResult>()

    @BeforeEach
    fun setUp() {
      whenever(clientDependencies.bigQuery.query(any<QueryJobConfiguration>(),
        any<JobId>())
      ).thenReturn(result)
      whenever(result.iterateAll())
        .thenReturn(listOf(FieldValueList.of(listOf(entityTest_result()), createField("entity",
          LegacySQLTypeName.RECORD, *entityTest_fields().toTypedArray()))))
    }

    @Nested
    inner class Single {
      @BeforeEach
      fun setUp() {
        whenever(result.iterateAll())
          .thenReturn(listOf(FieldValueList.of(listOf(single_result()), single_field())))
      }

      @Test
      fun `batchSingle returns entity`() {
        val actual = client.batchSingle(Long::class, "select 1")

        assertNotNull(actual)
        assertInstanceOf(Long::class.javaObjectType, actual)
      }

      @Test
      fun `tryBatchSingle returns entity`() {
        whenever(clientDependencies.bigQuery.create(any<JobInfo>())).thenReturn(mock())

        val actual = client.tryBatchSingle(Long::class, "select 1")

        assertNotNull(actual)
        assertInstanceOf(Long::class.javaObjectType, actual)
      }
    }

    @Test
    fun `batchEntity returns entity`() {
      val actual = client.batchEntity(EntityTest::class, "select 1")

      assertNotNull(actual)
      assertInstanceOf(EntityTest::class.java, actual)
    }

    @Test
    fun `tryBatchEntity returns entity`() {
      whenever(clientDependencies.bigQuery.create(any<JobInfo>())).thenReturn(mock())

      val actual = client.tryBatchEntity(EntityTest::class, "select 1")

      assertNotNull(actual)
      assertInstanceOf(EntityTest::class.java, actual)
    }

    @Test
    fun `batchRecord returns Map`() {

      val actual = client.batchRecord("select 1")

      assertNotNull(actual)
      assertInstanceOf(Map::class.java, actual)
    }

    @Test
    fun `tryBatchRecord returns Map`() {
      whenever(clientDependencies.bigQuery.create(any<JobInfo>())).thenReturn(mock())

      val actual = client.tryBatchRecord("select 1")

      assertNotNull(actual)
      assertInstanceOf(Map::class.java, actual)

    }

    @Test
    fun `batch methods reuse query path`() {
      whenever(clientDependencies.bigQuery.query(any<QueryJobConfiguration>(), any<JobId>())).thenReturn(result)

      assertEquals(result, client.batch("select 1"))
      verify(clientDependencies.bigQuery).query(any<QueryJobConfiguration>(), any<JobId>())
    }
  }
}