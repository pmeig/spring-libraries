package pmeig.spring.libraries.jpa.data.bigquery

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
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMapperFactory
import pmeig.spring.libraries.jpa.data.bigquery.mapper.factory.BigQueryMetadataFactory
import kotlin.reflect.KClass

private val logger = getLogger(BigQueryClient::class.java)

@Suppress("unused")
@Component
class BigQueryClient(
  private val bigQuery: BigQuery,
  private val jobId: JobId.Builder,
  private val properties: GcpBigQueryProperties,
  entityAnnotationReader: EntityAnnotationReader,
  mapperFactory: BigQueryMapperFactory,
) : BigQueryMultiClient(mapperFactory, entityAnnotationReader) {

  fun getTable(table: String, dataset: String = "", project: String = "")
  = getTable(createTableId(table, dataset, project))

  fun getTable(table: TableId): TableDefinition? = bigQuery.getTable(table).getDefinition()

  fun exists(table: TableId): Boolean = bigQuery.getTable(table).exists()
  fun exists(table: String, dataset: String = "", project: String = ""): Boolean =
    exists(createTableId(table, dataset, project))

  fun createTable(schema: Schema, table: TableId): Boolean {
    val tableDefinition = StandardTableDefinition.of(schema)
    return bigQuery.create(TableInfo.newBuilder(table, tableDefinition).build()).exists()
  }
  fun createTable(schema: Schema, table: String, dataset: String = "", project: String = "")
  = createTable(schema, createTableId(table, dataset, project))

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
    return bigQuery.query(configurator(QueryJobConfiguration.newBuilder(sql).setAllowLargeResults(true)).build(), jobId.setRandomJob().build())
  }

  fun <T : Any> tryEntity(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntity(entityRef.kotlin, sql, configurator)

  fun <T : Any> tryEntity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    tryEntities(entityRef, sql, configurator).firstOrNull()

  fun <T : Any> entity(
    entityRef: Class<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entity(entityRef.kotlin, sql, configurator)

  fun <T : Any> entity(
    entityRef: KClass<T>,
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ) =
    entities(entityRef, sql, configurator).firstOrNull()

  fun tryJson(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? {
    return toJson(sql) { tryQuery(it, configurator) }
  }

  fun json(
    sql: String,
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): String? {
    return toJson(sql) { query(it, configurator) }
  }

  fun tryRecord(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> =
    tryRecords(sql, metadataFactory, configurator).firstOrNull() ?: emptyMap()

  fun record(
    sql: String,
    metadataFactory: Map<String, BigQueryMetadataFactory> = emptyMap(),
    configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }
  ): Map<String, Any?> = records(sql, metadataFactory, configurator).firstOrNull() ?: emptyMap()

  fun createTableId(table: String, dataset: String = "", project: String = ""): TableId =
    TableId.of(project.ifEmpty { properties.projectId }, dataset.ifEmpty { properties.datasetName }, table)

  private fun toJson(sql: String, executor: (sql: String) -> TableResult?): String? {
    val toJsonQuery = "WITH request AS ($sql) " +
            "SELECT to_json_string(request) AS json FROM request request"
    return exec({
      executor(toJsonQuery)
    }) {
      it.iterateAll().firstOrNull()?.get("json")?.stringValue
    }
  }

}