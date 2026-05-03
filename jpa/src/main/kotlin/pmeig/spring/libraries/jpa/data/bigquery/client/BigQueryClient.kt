package pmeig.spring.libraries.jpa.data.bigquery.client

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TableResult
import com.google.cloud.spring.autoconfigure.bigquery.GcpBigQueryProperties
import org.slf4j.LoggerFactory.getLogger
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.BigQuerySqlMapper
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import java.time.Duration
import kotlin.reflect.KClass

private val logger = getLogger(BigQueryClient::class.java)

@Suppress("unused")
@Component
class BigQueryClient(
  private val bigQuery: BigQuery,
  private val jobId: JobId.Builder,
  private val properties: GcpBigQueryProperties,
  entityAnnotationReader: EntityAnnotationReader,
  bigQuerySqlMapper: BigQuerySqlMapper,
  cacheManager: CacheManager,
  mapperFactory: BigQueryMapperFactory,
) : BigQueryClientSimple(
  entityAnnotationReader,
  bigQuerySqlMapper,
  cacheManager,
  mapperFactory
) {

  @JvmOverloads
  fun getTable(table: String, dataset: String = "", project: String = "")
          = getTable(createTableId(table, dataset, project))

  fun getTable(table: TableId): TableDefinition? = bigQuery.getTable(table).getDefinition()

  fun exists(table: TableId): Boolean = bigQuery.getTable(table).exists()
  @JvmOverloads
  fun exists(table: String, dataset: String = "", project: String = ""): Boolean =
    exists(createTableId(table, dataset, project))

  @JvmOverloads
  fun createTable(schema: Schema, table: TableId, expiration: Duration? = null): Boolean {
    val tableDefinition = StandardTableDefinition.of(schema)
    var tableInfoBuilder = TableInfo.newBuilder(table, tableDefinition)
    expiration?.let {
      tableInfoBuilder = tableInfoBuilder.setExpirationTime(it.toMillis())
    }
    return bigQuery.create(tableInfoBuilder.build()).exists()
  }
  @JvmOverloads
  fun createTable(schema: Schema, table: String, dataset: String = "", project: String = "", expiration: Duration? = null)
          = createTable(schema, createTableId(table, dataset, project), expiration)

  @JvmOverloads
  fun drop(table: String, dataset: String = "", project: String = "") = drop(createTableId(table, dataset, project))
  fun drop(table: TableId) = bigQuery.delete(table)

  override fun tryQuery(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder
  ): TableResult? {
    return try {
      val configuration = configurator(QueryJobConfiguration.newBuilder(sql).setAllowLargeResults(true))
      bigQuery.create(JobInfo.of(configuration.setDryRun(true).build()))
      bigQuery.query(configuration.setDryRun(false).build(), jobId.setRandomJob().build())
    } catch (e: Exception) {
      logger.warn("Execution failed", e)
      null
    }
  }

  override fun query(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder
  ): TableResult {
    return bigQuery.query(
      configurator(QueryJobConfiguration.newBuilder(sql).setAllowLargeResults(true)).build(),
      jobId.setRandomJob().build()
    )
  }

  fun batch(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) = query(sql, toBatch(configurator))

  fun tryBatch(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) = tryQuery(sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> tryBatchEntity(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntity(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> tryBatchEntity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntity(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> batchEntity(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entity(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun <T : Any> batchEntity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entity(entityRef, sql, toBatch(configurator))

  @JvmOverloads
  fun tryBatchJson(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? =
    tryJson(sql, toBatch(configurator))

  @JvmOverloads
  fun batchJson(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? =
    json(sql, toBatch(configurator))

  @JvmOverloads
  fun tryBatchRecord(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> =
    tryRecord(sql, metadataFactory, toBatch(configurator))

  @JvmOverloads
  fun batchRecord(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> =
    record(sql, metadataFactory, toBatch(configurator))

  @JvmOverloads
  fun createTableId(table: String, dataset: String = "", project: String = ""): TableId =
    TableId.of(project.ifEmpty { properties.projectId }, dataset.ifEmpty { properties.datasetName }, table)

}
