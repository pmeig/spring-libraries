package pmeig.spring.libraries.jpa.data.bigquery.v2

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.JobId
import com.google.cloud.bigquery.JobInfo
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import pmeig.spring.libraries.jpa.core.entity.EntityAnnotationReader
import pmeig.spring.libraries.jpa.data.bigquery.v2.mapper.factory.BigQueryMapperFactory

private val logger = getLogger(BigQueryClient::class.java)

@Component
class BigQueryClient(
  private val bigQuery: BigQuery,
  private val jobId: JobId.Builder,
  entityAnnotationReader: EntityAnnotationReader,
  mapperFactory: BigQueryMapperFactory,
): BigQueryMultiClient(mapperFactory, entityAnnotationReader) {

  override fun tryQuery(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): TableResult? {
    return try {
      val configuration = configurator(QueryJobConfiguration.newBuilder(sql))
      bigQuery.create(JobInfo.of(configuration.setDryRun(true).build()))
      bigQuery.query(configuration.setDryRun(false).build(), jobId.setRandomJob().build())
    } catch (e: Exception) {
      logger.warn("Execution failed", e)
      null
    }
  }

  override fun query(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder): TableResult {
    return bigQuery.query(configurator(QueryJobConfiguration.newBuilder(sql)).build(), jobId.setRandomJob().build())
  }

  fun tryJson(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }): String? {
    return toJson(sql) { tryQuery(it, configurator) }
  }

  fun json(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = { it }): String? {
    return toJson(sql) { query(it, configurator) }
  }

  fun tryRecord(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = {it}): Map<String, Any?> = recorded {
    tryQuery(sql, configurator)
  }?.firstOrNull() ?: emptyMap()

  fun record(sql: String, configurator: (QueryJobConfiguration.Builder) -> QueryJobConfiguration.Builder = {it}): Map<String, Any?>
  = recorded { query(sql, configurator) }?.firstOrNull() ?: emptyMap()

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